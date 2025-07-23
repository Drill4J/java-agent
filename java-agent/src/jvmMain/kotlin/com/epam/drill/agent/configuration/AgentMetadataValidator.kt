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

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.konform.validation.Invalid
import com.epam.drill.agent.konform.validation.Validation
import com.epam.drill.agent.konform.validation.ValidationError
import com.epam.drill.agent.konform.validation.ValidationResult
import com.epam.drill.agent.konform.validation.jsonschema.minItems
import com.epam.drill.agent.konform.validation.jsonschema.minLength
import com.epam.drill.agent.konform.validation.jsonschema.minimum
import com.epam.drill.agent.konform.validation.jsonschema.pattern
import mu.KotlinLogging

object AgentMetadataValidator {
    private val logger = KotlinLogging.logger {}

    private class ValidatingParameters(parameters: AgentParameters) {
        val appId = parameters[DefaultParameterDefinitions.APP_ID]
        val groupId = parameters[DefaultParameterDefinitions.GROUP_ID]
        val buildVersion = parameters[DefaultParameterDefinitions.BUILD_VERSION]
        val commitSha = parameters[DefaultParameterDefinitions.COMMIT_SHA]
        val envId: String? = parameters[DefaultParameterDefinitions.ENV_ID]
        val packagePrefixes = parameters[DefaultParameterDefinitions.PACKAGE_PREFIXES]
        val apiUrl = parameters[ParameterDefinitions.API_URL]
        val apiKey = parameters[ParameterDefinitions.API_KEY]
        val logLevel = parameters[ParameterDefinitions.LOG_LEVEL]
        val logLevelAsList = logLevel.split(";")
        val logLimit = parameters[ParameterDefinitions.LOG_LIMIT]
    }

    private val strictValidators = Validation<ValidatingParameters> {
        ValidatingParameters::groupId required {
            identifier()
            minLength(3)
        }
        ValidatingParameters::appId required {
            identifier()
            minLength(3)
        }
        ValidatingParameters::apiUrl required {
            validTransportUrl()
        }
        ValidatingParameters::packagePrefixes {
            minItems(1)
        }
        ValidatingParameters::packagePrefixes onEach {
            isValidPackage()
        }
    }

    private val softValidators = Validation<ValidatingParameters> {
        ValidatingParameters::buildVersion ifPresent {
            pattern("^\\S*$") hint "must not contain whitespaces"
        }
        ValidatingParameters::commitSha ifPresent {
            pattern("^[a-f0-9]{40}\$") hint "must be a valid full commit SHA"
        }
        ValidatingParameters::envId ifPresent {
            minLength(1)
        }
        ValidatingParameters::apiKey ifPresent {
            minLength(1)
        }
        ValidatingParameters::logLevelAsList onEach {
            isValidLogLevel()
        }
        ValidatingParameters::logLimit ifPresent {
            minimum(0)
        }
    }

    fun validate(parameters: AgentParameters): Map<String, String> {
        val defaultValues: MutableMap<String, String> = mutableMapOf()
        val defaultFor: (AgentParameterDefinition<out Any>) -> Unit = {
            defaultValues[it.name] = it.defaultValue.toString()
        }
        val isInvalid: (ValidationResult<*>) -> Boolean = { it is Invalid }
        strictValidators(ValidatingParameters(parameters)).takeIf(isInvalid)?.let { result ->
            val message = "Cannot load the agent because some agent parameters are set incorrectly. " +
                    convertToMessage(result.errors)

            throw java.lang.IllegalArgumentException(message)
        }
        softValidators(ValidatingParameters(parameters)).takeIf(isInvalid)?.let { result ->
            val message = "Some agent parameters were set incorrectly and were replaced with default values. " +
                    convertToMessage(result.errors)
            logger.error { message }
            result.errors.forEach { error ->
                when (convertToField(error)) {
                    ValidatingParameters::logLevel.name -> defaultFor(ParameterDefinitions.LOG_LEVEL)
                    ValidatingParameters::logLimit.name -> defaultFor(ParameterDefinitions.LOG_LIMIT)
                }
            }
        }
        return defaultValues
    }



    private fun convertToMessage(errors: List<ValidationError>) = "Please check the following parameters:\n" +
            errors.joinToString("\n") { " - ${convertToField(it)} ${it.message.removeExtraValues()}" }

    private fun convertToField(error: ValidationError) = error.dataPath.removePrefix(".")
        .substringBeforeLast("AsList")
        .removeSuffix("AsInt")

    //TODO: figure out why Konform adds this prefixes to a message
    private fun String.removeExtraValues() = this
        .removePrefix("com.epam.drill.agent.shadow.")
        .removePrefix("com/epam/drill/agent/shadow/")

}