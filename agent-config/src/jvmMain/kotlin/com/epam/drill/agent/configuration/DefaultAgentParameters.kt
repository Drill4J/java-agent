/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.configuration

import kotlin.reflect.KProperty
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.BaseAgentParameterDefinition
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition
import com.epam.drill.agent.common.configuration.ValidationError
import com.epam.drill.agent.konform.validation.Invalid
import com.epam.drill.agent.konform.validation.Valid
import com.epam.drill.agent.konform.validation.ValidationResult
import mu.KotlinLogging
import kotlin.let

actual class DefaultAgentParameters actual constructor(
    private val inputParameters: Map<String, String>
) : AgentParameters {
    private val logger = KotlinLogging.logger {}
    private val definedParameters = mutableMapOf<String, Any?>()
    private val parameterDefinitions = mutableMapOf<String, BaseAgentParameterDefinition<*>>()
    private val validationErrors = mutableMapOf<String, ValidationError<*>>()

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(name: String): T? =
        definedParameters[name] as T?

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        if (!parameterDefinitions.containsKey(definition.name)) define(definition)
        return definedParameters[definition.name] as T?
            ?: validationErrors[definition.name]?.let {
                throw IllegalArgumentException(
                    "Parameter '${definition.name}' has validation errors: \n" +
                            it.messages.joinToString("\n")
                )
            }
            ?: throw IllegalArgumentException(
                "Parameter '${definition.name}' has no value."
            )
    }

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T? =
        definedParameters[property.name] as T?

    @Suppress("UNCHECKED_CAST")
    actual override fun <T : Any> get(definition: NullableAgentParameterDefinition<T>): T? {
        if (!parameterDefinitions.containsKey(definition.name)) define(definition)
        return definedParameters[definition.name] as T?
    }

    actual override fun define(vararg definitions: BaseAgentParameterDefinition<*>): List<ValidationError<*>> {
        val errors = mutableListOf<ValidationError<*>>()
        definitions.forEach { def ->
            if (parameterDefinitions.containsKey(def.name)) {
                validationErrors[def.name]?.also {
                    errors.add(it)
                }
                return@forEach
            }
            parameterDefinitions[def.name] = def
            definedParameters[def.name] = (inputParameters[def.name]
                ?.runCatching(def.parser)
                ?.getOrNull()
                ?.let { softValidate(it, def, errors) }
                ?: (def as? AgentParameterDefinition)?.defaultValue)
                .let { strictValidate(it, def, errors) }
            errors.firstOrNull { it.definition.name == def.name }?.also {
                validationErrors[def.name] = it
            } ?: validationErrors.remove(def.name)
        }
        return errors
    }

    private fun strictValidate(
        value: Any?,
        definition: BaseAgentParameterDefinition<*>,
        errors: MutableList<ValidationError<*>> = mutableListOf()
    ): Any? = if (definition.isStrictValidation()) validate(value, definition, errors) else value

    private fun softValidate(
        value: Any?,
        definition: BaseAgentParameterDefinition<*>,
        errors: MutableList<ValidationError<*>> = mutableListOf()
    ): Any? = if (definition.isSoftValidation()) validate(value, definition, errors) else value

    private fun validate(
        value: Any?,
        definition: BaseAgentParameterDefinition<*>,
        errors: MutableList<ValidationError<*>> = mutableListOf()
    ): Any? {
        val result = safeValidate(value, definition.validator)
        when (result) {
            is Invalid -> {
                errors.add(ValidationError(definition, result.errors.map { it.message }))
                logger.debug { "Validation failed for parameter '${definition.name}': ${result.errors.map { it.message }}" }
                return null
            }

            is Valid -> {
                return value
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> safeValidate(
        value: Any?,
        validator: (T?, AgentParameters) -> ValidationResult<*>
    ): ValidationResult<*> {
        val typedValue = value as T?
        return validator(typedValue, this)
    }
}
