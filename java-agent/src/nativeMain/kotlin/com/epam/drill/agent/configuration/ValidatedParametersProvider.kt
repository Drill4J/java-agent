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
import mu.KotlinLogging
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.konform.validation.Invalid
import com.epam.drill.agent.konform.validation.Validation
import com.epam.drill.agent.konform.validation.ValidationError
import com.epam.drill.agent.konform.validation.ValidationErrors
import com.epam.drill.agent.konform.validation.ValidationResult
import com.epam.drill.agent.konform.validation.jsonschema.minItems
import com.epam.drill.agent.konform.validation.jsonschema.minLength
import com.epam.drill.agent.konform.validation.jsonschema.minimum
import com.epam.drill.agent.konform.validation.jsonschema.pattern

class ValidatedParametersProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = Int.MAX_VALUE
) : AgentConfigurationProvider {

    private class ValidatingParameters(provider: ValidatedParametersProvider) {
        val appId by provider
        val groupId by provider
        val buildVersion by provider
        val commitSha by provider
        val envId by provider
        val packagePrefixes by provider
        val packagePrefixesAsList = packagePrefixes?.split(";") ?: emptyList()
        val drillInstallationDir by provider
        val apiUrl by provider
        val apiKey by provider
        val logLevel by provider
        val logLevelAsList = logLevel?.split(";") ?: emptyList()
        val logLimit by provider
        val logLimitAsInt = logLimit?.toIntOrNull()
    }

    private val strictValidators = Validation<ValidatingParameters> {
        ValidatingParameters::drillInstallationDir required {
            minLength(1)
        }
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
        ValidatingParameters::packagePrefixesAsList {
            minItems(1)
        }
        ValidatingParameters::packagePrefixesAsList onEach {
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
        ValidatingParameters::logLimitAsInt ifPresent {
            minimum(0)
        }
    }

    private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.ValidatedParametersProvider")

    private val validatingConfiguration = validatingConfiguration()

    override val configuration
        get() = validateConfiguration()

    fun validate(): List<ValidationError> {
        val strictValidationErrors = strictValidators(ValidatingParameters(this)).takeIf { it is Invalid }?.errors?.toList() ?: emptyList()
        val softValidationErrors = softValidators(ValidatingParameters(this)).takeIf { it is Invalid }?.errors?.toList() ?: emptyList()
        return strictValidationErrors + softValidationErrors
    }

    internal fun validatingConfiguration() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .map(AgentConfigurationProvider::configuration)
        .reduce { acc, map -> acc + map }

    private fun validateConfiguration() = mutableMapOf<String, String>().also { defaultValues ->
        val defaultFor: (AgentParameterDefinition<out Any>) -> Unit = {
            defaultValues[it.name] = it.defaultValue.toString()
        }
        val isInvalid: (ValidationResult<*>) -> Boolean = { it is Invalid }
        strictValidators(ValidatingParameters(this)).takeIf(isInvalid)?.let { result ->
            val message = "Cannot load the agent because some agent parameters are set incorrectly. " +
                    convertToMessage(result.errors)
            logger.error { message }
            throw ParameterValidationException(message)
        }
        softValidators(ValidatingParameters(this)).takeIf(isInvalid)?.let { result ->
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
    }

    private fun convertToMessage(errors: ValidationErrors) = "Please check the following parameters:\n" +
            errors.joinToString("\n") { " - ${convertToField(it)} ${it.message}" }

    private fun convertToField(error: ValidationError) = error.dataPath.removePrefix(".")
        .substringBeforeLast("AsList")
        .removeSuffix("AsInt")

    private operator fun getValue(thisRef: Any, property: KProperty<*>) =
        validatingConfiguration[property.name]

}
