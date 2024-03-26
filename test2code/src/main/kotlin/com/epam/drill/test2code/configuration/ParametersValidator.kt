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
package com.epam.drill.test2code.configuration

import kotlin.time.Duration
import mu.KotlinLogging
import com.epam.drill.common.agent.configuration.AgentParameters
import com.epam.drill.konform.validation.Invalid
import com.epam.drill.konform.validation.Validation
import com.epam.drill.konform.validation.ValidationError
import com.epam.drill.konform.validation.ValidationErrors
import com.epam.drill.konform.validation.ValidationResult

object ParametersValidator {

    private class ValidatingParameters(parameters: AgentParameters) {
        val classScanDelay = parameters[ParameterDefinitions.CLASS_SCAN_DELAY]
        val scanClassPath = parameters[ParameterDefinitions.SCAN_CLASS_PATH]
    }

    private val softValidators = Validation<ValidatingParameters> {
        ValidatingParameters::classScanDelay ifPresent {
            minimum(Duration.ZERO)
        }
        ValidatingParameters::scanClassPath onEach {
            pathExists()
        }
    }

    private val logger = KotlinLogging.logger {}

    fun validate(parameters: AgentParameters) {
        val isInvalid: (ValidationResult<*>) -> Boolean = { it is Invalid }
        softValidators(ValidatingParameters(parameters)).takeIf(isInvalid)?.let { result ->
            val message = "Some agent parameters were set incorrectly and were replaced with default values. " +
                    convertToMessage(result.errors)
            logger.error { message }
        }
    }

    private fun convertToMessage(errors: ValidationErrors) = "Please check the following parameters:\n" +
            errors.joinToString("\n") { " - ${convertToField(it)} ${it.message}" }

    private fun convertToField(error: ValidationError) = error.dataPath.removePrefix(".")

}
