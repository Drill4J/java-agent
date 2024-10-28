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

import com.epam.drill.jvmapi.callObjectIntMethod
import com.epam.drill.jvmapi.callObjectStringMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.logging.LoggingConfiguration
import kotlinx.cinterop.ExperimentalForeignApi

object AgentLoggingConfiguration {

    fun defaultNativeLoggingConfiguration() {
        LoggingConfiguration.readDefaultConfiguration()
    }

    fun updateNativeLoggingConfiguration() {
        val logLevel = Configuration.parameters[ParameterDefinitions.LOG_LEVEL]
        val logFile = Configuration.parameters[ParameterDefinitions.LOG_FILE].takeIf(String::isNotEmpty)
        val logLimit = Configuration.parameters[ParameterDefinitions.LOG_LIMIT]

        LoggingConfiguration.setLoggingLevels(logLevel)
        if (LoggingConfiguration.getLoggingFilename() != logFile) {
            LoggingConfiguration.setLoggingFilename(logFile)
        }
        if (LoggingConfiguration.getLogMessageLimit() != logLimit) {
            LoggingConfiguration.setLogMessageLimit(logLimit)
        }
    }

    fun defaultJvmLoggingConfiguration() {
        callObjectVoidMethod(LoggingConfiguration::class, LoggingConfiguration::readDefaultConfiguration)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun updateJvmLoggingConfiguration() {
        val logLevel = Configuration.parameters[ParameterDefinitions.LOG_LEVEL]
        val logFile = Configuration.parameters[ParameterDefinitions.LOG_FILE].takeIf(String::isNotEmpty)
        val logLimit = Configuration.parameters[ParameterDefinitions.LOG_LIMIT]

        callObjectVoidMethodWithString(LoggingConfiguration::class, "setLoggingLevels", logLevel)
        if (callObjectStringMethod(LoggingConfiguration::class,LoggingConfiguration::getLoggingFilename) != logFile) {
            callObjectVoidMethodWithString(
                LoggingConfiguration::class,
                LoggingConfiguration::setLoggingFilename,
                logFile
            )
        }
        if (callObjectIntMethod(LoggingConfiguration::class, LoggingConfiguration::getLogMessageLimit) != logLimit) {
            callObjectVoidMethodWithInt(
                LoggingConfiguration::class,
                LoggingConfiguration::setLogMessageLimit,
                logLimit
            )
        }
    }

}
