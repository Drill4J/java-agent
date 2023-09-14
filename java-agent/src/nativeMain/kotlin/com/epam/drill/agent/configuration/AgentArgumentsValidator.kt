/**
 * Copyright 2020 - 2023 EPAM Systems
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

import com.epam.drill.agent.configuration.exceptions.AgentValidationException
import com.epam.drill.konform.validation.Invalid
import com.epam.drill.konform.validation.Validation
import com.epam.drill.konform.validation.ValidationErrors
import com.epam.drill.konform.validation.jsonschema.*
import mu.KotlinLogging
import mu.KotlinLoggingLevel
import kotlin.native.concurrent.SharedImmutable


@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration")

object AgentArgumentsValidator {

    val strictValidators = Validation<AgentArguments> {
        AgentArguments::drillInstallationDir required {
            pathExists()
        }
        AgentArguments::agentId required {
            identifier()
            minLength(3)
        }
        AgentArguments::buildVersion required {
            pattern("^\\S*$") hint "must not contain whitespaces"
        }
        AgentArguments::adminAddress required {
            hostAndPort()
        }
        AgentArguments::packagePrefixesToList {
            minItems(1)
        }
        AgentArguments::packagePrefixesToList onEach {
            isValidPackage()
        }
    }
    val softValidators = Validation<AgentArguments> {
        AgentArguments::groupId ifPresent {
            identifier()
            minLength(3)
        }
        AgentArguments::classScanDelay ifPresent {
            minimum(0)
        }
        AgentArguments::scanClassPathToList onEach {
            pathExists()
        }
        AgentArguments::logLevel ifPresent {
            enum<KotlinLoggingLevel>()
        }
        AgentArguments::logLimit ifPresent {
            minimum(0)
        }
    }

    fun validate(args: AgentArguments) {
        strictValidators(args).takeIf { it is Invalid }?.let { result ->
            val message = "Can’t load the agent because some agent parameters are set incorrectly. " +
                    convertToMessage(result.errors)
            logger.error { message }
            throw AgentValidationException(
                message
            )
        }
        softValidators(args).takeIf { it is Invalid }?.let { result ->
            logger.error {
                "Some agent parameters were set incorrectly and were replaced with default values. " +
                        convertToMessage(result.errors)
            }
            val defaultValue = AgentArguments()
            result.errors.forEach { error ->
                when (convertToField(error.dataPath)) {
                    AgentArguments::groupId.name -> args.groupId = defaultValue.groupId
                    AgentArguments::classScanDelay.name -> args.classScanDelay = defaultValue.classScanDelay
                    AgentArguments::logLevel.name -> args.logLevel = defaultValue.logLevel
                    AgentArguments::logLimit.name -> args.logLimit = defaultValue.logLimit
                }
            }
        }
    }

    private fun convertToMessage(errors: ValidationErrors) = "Please check the following parameters:\n" +
            errors.joinToString("\n") { " - ${convertToField(it.dataPath)} ${it.message}" }

    private fun convertToField(dataPath: String) = dataPath.replaceFirst(".", "")
        .replace("ToList", "")
}