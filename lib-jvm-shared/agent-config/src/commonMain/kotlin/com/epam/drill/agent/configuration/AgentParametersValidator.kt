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

import com.epam.drill.agent.common.configuration.AgentParameterDefinitionCollection
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.BaseAgentParameterDefinition
import com.epam.drill.agent.common.configuration.ValidationError
import mu.KotlinLogging

class AgentParametersValidator(private val parameters: AgentParameters) {
    private val logger = KotlinLogging.logger { }

    fun validate(vararg collections: AgentParameterDefinitionCollection) {
        validate(*collections.flatMap { it.getAll() }.toTypedArray())
    }

    fun validate(vararg definitions: BaseAgentParameterDefinition<*>) {
        getStrictValidationErrors(*definitions)
            .takeIf { it.isNotEmpty() }
            ?.let { errors ->
                throw AgentParameterValidationError(
                    "Some parameters are set incorrectly.\n" +
                            "Please check the following parameters:\n" +
                            errors.joinToString("\n")
                )
            }
        getSoftValidationErrors(*definitions)
            .takeIf { it.isNotEmpty() }
            ?.let { errors ->
                logger.warn {
                    "Some parameters were set incorrectly and were replaced with default values.\n" +
                            "Please check the following parameters:\n" +
                            errors.joinToString("\n")
                }
            }
    }

    fun getValidationErrors(vararg definitions: BaseAgentParameterDefinition<*>): List<String> {
        return parameters.define(*definitions).flatMap(::formatErrors)
    }

    fun getStrictValidationErrors(vararg definitions: BaseAgentParameterDefinition<*>): List<String> {
        return getValidationErrors(*definitions.filter { it.isStrictValidation() }.toTypedArray())
    }

    fun getSoftValidationErrors(vararg definitions: BaseAgentParameterDefinition<*>): List<String> {
        return getValidationErrors(*definitions.filter { it.isSoftValidation() }.toTypedArray())
    }

    private fun formatErrors(error: ValidationError<*>): List<String> =
        error.messages.associateBy { error.definition }.map {
            "- ${it.key.name} ${it.value.removeRelocationPackage()}. ${it.key.description?: ""}\n"
        }

    //TODO: workaround package relocation issue with konform
    private fun String.removeRelocationPackage() = this
        .removePrefix("com.epam.drill.agent.shadow.")
        .removePrefix("com/epam/drill/agent/shadow/")
}