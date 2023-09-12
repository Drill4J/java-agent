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

import com.epam.drill.agent.configuration.exceptions.StrictValidationException
import com.epam.drill.konform.validation.Invalid
import com.epam.drill.konform.validation.Validation
import com.epam.drill.konform.validation.ValidationResult
import com.epam.drill.konform.validation.jsonschema.enum
import com.epam.drill.konform.validation.jsonschema.minLength
import com.epam.drill.konform.validation.jsonschema.minimum
import mu.KotlinLogging
import mu.KotlinLoggingLevel

object AgentArgumentsValidator {
    private val logger = KotlinLogging.logger {}

    val strictValidators = Validation<AgentArguments> {
        AgentArguments::agentId required {
            identifier()
            minLength(3)
        }
        AgentArguments::buildVersion required {

        }
        AgentArguments::adminAddress required {
            hostAndPort()
        }
        AgentArguments::packagePrefixes required {

        }
    }
    val softValidators = Validation<AgentArguments> {
        AgentArguments::groupId ifPresent {
            identifier()
            minLength(3)
        }
        AgentArguments::instanceId ifPresent {

        }
        AgentArguments::drillInstallationDir ifPresent {
//            pathExists()
        }
        AgentArguments::classScanDelay ifPresent {
            minimum(0)
        }
        AgentArguments::scanClassPath ifPresent {
//            pathExists() //TODO ?
        }
        AgentArguments::logLevel ifPresent {
            enum("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
        }
        AgentArguments::logLimit ifPresent {
            minimum(0)
        }
        AgentArguments::logFile ifPresent {
//            fileExists() TODO change to valid path
        }
    }

    fun validate(args: AgentArguments) {
        strictValidators(args).takeIf { it is Invalid }?.let {
            throw StrictValidationException(mapToMessage(it))
        }
        softValidators(args).takeIf { it is Invalid }?.let { result ->
            logger.warn { "Some parameters were set incorrectly and were replaced with default values. Please check next parameters:\n\r${mapToMessage(result)}\""}
            val defaultValue = AgentArguments()
            result.errors.forEach { error ->
                when(mapToField(error.dataPath)) {
                    AgentArguments::groupId.name -> defaultValue.groupId
                    AgentArguments::instanceId.name -> defaultValue.instanceId
                    AgentArguments::drillInstallationDir.name -> defaultValue.drillInstallationDir
                    AgentArguments::logLevel.name -> args.logLevel = defaultValue.logLevel
                    AgentArguments::logFile.name -> args.logFile = defaultValue.logFile
                    AgentArguments::logLimit.name -> args.logLimit = defaultValue.logLimit
                    AgentArguments::classScanDelay.name -> args.classScanDelay = defaultValue.classScanDelay
                    AgentArguments::scanClassPath.name -> args.scanClassPath = defaultValue.scanClassPath
                }
            }
        }
    }

    private fun mapToMessage(validationResult: ValidationResult<AgentArguments>): String {
        return validationResult.errors.joinToString("\n\r") { " - ${mapToField(it.dataPath)} ${it.message}" }
    }

    private fun mapToField(dataPath: String) = dataPath.replaceFirst(".", "")
}