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
package com.epam.drill.agent.common.configuration

import com.epam.drill.agent.konform.validation.Valid
import com.epam.drill.agent.konform.validation.Validation
import com.epam.drill.agent.konform.validation.ValidationBuilder
import com.epam.drill.agent.konform.validation.ValidationResult
import kotlin.String
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AgentParameterDefinition<T : Any>(
    name: String,
    description: String? = null,
    type: KClass<T>,
    val defaultValue: T? = null,
    parser: (String) -> T,
    validation: ValidationType = ValidationType.STRICT,
    validator: (T?, AgentParameters) -> ValidationResult<*>
) : BaseAgentParameterDefinition<T>(
    name = name,
    description = description,
    type = type,
    parser = parser,
    validation = validation,
    validator = validator,
) {

    companion object {

        fun forString(
            name: String,
            description: String? = null,
            defaultValue: String? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> String = { it },
            validator: ValidationBuilder<String>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            description,
            String::class,
            defaultValue,
            parser,
            validation,
            validator = { it, _ -> validate( it, validator) }
        )

        fun forBoolean(
            name: String,
            description: String? = null,
            defaultValue: Boolean? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Boolean = { it.toBoolean() },
            validator: ValidationBuilder<Boolean>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            description,
            Boolean::class,
            defaultValue,
            parser,
            validation,
            validator = { it, _ -> validate( it, validator) }
        )

        fun forInt(
            name: String,
            description: String? = null,
            defaultValue: Int? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Int = { it.toInt() },
            validator: ValidationBuilder<Int>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            description,
            Int::class,
            defaultValue,
            parser,
            validation,
            validator = { it, _ -> validate( it, validator) }
        )

        fun forLong(
            name: String,
            description: String? = null,
            defaultValue: Long? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Long = { it.toLong() },
            validator: ValidationBuilder<Long>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            description,
            Long::class,
            defaultValue,
            parser,
            validation,
            validator = { it, _ -> validate( it, validator) }
        )

        fun forDuration(
            name: String,
            description: String? = null,
            defaultValue: Duration? = null,
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> Duration = { it.toLong().toDuration(DurationUnit.MILLISECONDS) },
            validator: ValidationBuilder<Duration>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name,
            description,
            Duration::class,
            defaultValue,
            parser,
            validation,
            validator = { it, _ -> validate( it, validator) }
        )

        fun forList(
            name: String,
            description: String? = null,
            requiredIf: (AgentParameters) -> Boolean = { false },
            defaultValue: List<String> = emptyList(),
            validation: ValidationType = ValidationType.STRICT,
            parser: (String) -> List<String> = { it.split(";") },
            listValidator: ValidationBuilder<List<String>>.() -> Unit = {},
            itemValidator: ValidationBuilder<String>.() -> Unit = {}
        ) = AgentParameterDefinition(
            name = name,
            description = description,
            type = List::class,
            defaultValue = defaultValue,
            parser = parser,
            validation = validation,
            validator = { value, params ->
                class Typed(val value: List<String>)
                Validation {
                    if (requiredIf(params))
                        Typed::value required listValidator
                    Typed::value onEach itemValidator
                }.validate(Typed(value?.filterIsInstance<String>() ?: emptyList()))
            }
        )
    }
}