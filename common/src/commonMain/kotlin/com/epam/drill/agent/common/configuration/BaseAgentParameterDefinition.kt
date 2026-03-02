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

import com.epam.drill.agent.konform.validation.Validation
import com.epam.drill.agent.konform.validation.ValidationBuilder
import com.epam.drill.agent.konform.validation.ValidationResult
import kotlin.reflect.KClass

sealed class BaseAgentParameterDefinition<T : Any>(
    val name: String,
    val description: String? = null,
    val type: KClass<T>,
    val parser: (String) -> T,
    val validation: ValidationType = ValidationType.STRICT,
    val validator: (T?, AgentParameters) -> ValidationResult<*>
) {
    fun isSoftValidation() = validation == ValidationType.SOFT
    fun isStrictValidation() = validation == ValidationType.STRICT
}

enum class ValidationType {
    STRICT,
    SOFT
}

internal fun <T> validateIfPresent(value: T?, validator: ValidationBuilder<T>.() -> Unit): ValidationResult<*> {
    class Typed(val value: T?)
    return Validation {
        Typed::value ifPresent validator
    }.validate(Typed(value))
}

internal fun <T> validate(value: T?, validator: ValidationBuilder<T>.() -> Unit): ValidationResult<*> {
    class Typed(val value: T?)
    return Validation {
        Typed::value required validator
    }.validate(Typed(value))
}

