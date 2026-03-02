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

import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
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
import kotlin.collections.set

actual class DefaultAgentParameters actual constructor(
    private val inputParameters: Map<String, String>
) : AgentParameters {
    private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.DefaultAgentParameters")
    private val definedParameters = AtomicReference(mapOf<String, Any?>())
    private val parameterDefinitions = AtomicReference(mapOf<String, BaseAgentParameterDefinition<*>>())
    private val validationErrors = AtomicReference(mapOf<String, ValidationError<*>>())

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(name: String): T? =
        definedParameters.value[name] as T?

    @Suppress("UNCHECKED_CAST")
    actual override operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T {
        if (!parameterDefinitions.value.containsKey(definition.name)) define(definition)
        return definedParameters.value[definition.name] as T?
            ?: validationErrors.value[definition.name]?.let {
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
        definedParameters.value[property.name] as T?


    actual override fun <T : Any> get(definition: NullableAgentParameterDefinition<T>): T? {
        if (!parameterDefinitions.value.containsKey(definition.name)) define(definition)
        return definedParameters.value[definition.name] as T?
    }

    actual override fun define(vararg definitions: BaseAgentParameterDefinition<out Any>): List<ValidationError<out Any>> {
        val updatedDefinitions = parameterDefinitions.value.toMutableMap()
        val updatedParameters = definedParameters.value.toMutableMap()
        val updatedValidationErrors = validationErrors.value.toMutableMap()
        val errors = mutableListOf<ValidationError<out Any>>()
        definitions.forEach { def ->
            if (updatedDefinitions.containsKey(def.name)) {
                updatedValidationErrors[def.name]?.also {
                    errors.add(it)
                }
                return@forEach
            }
            updatedDefinitions[def.name] = def
            updatedParameters[def.name] = (inputParameters[def.name]
                ?.runCatching(def.parser)
                ?.getOrNull()
                ?.let { softValidate(it, def, errors) }
                ?: (def as? AgentParameterDefinition)?.defaultValue)
                .let { strictValidate(it, def, errors) }
            errors.firstOrNull { it.definition.name == def.name }?.also {
                updatedValidationErrors[def.name] = it
            } ?: updatedValidationErrors.remove(def.name)
        }
        parameterDefinitions.value = updatedDefinitions.freeze()
        definedParameters.value = updatedParameters.freeze()
        validationErrors.value = updatedValidationErrors.freeze()
        return errors
    }

    private fun strictValidate(
        value: Any?,
        definition: BaseAgentParameterDefinition<*>,
        errors: MutableList<ValidationError<*>> = mutableListOf()
    ): Any? = if (definition.isStrictValidation()) validate(value, definition, errors) else value

    private fun softValidate(
        value: Any,
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
                val error = ValidationError(definition, result.errors.map { it.message })
                validationErrors.value
                errors.add(error)
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
