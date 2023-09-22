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

import kotlinx.serialization.Serializable
import mu.KotlinLoggingLevel
import com.epam.drill.agent.configuration.process.javaProcess
import com.epam.drill.common.agent.configuration.AgentParameter

@Serializable
data class AgentArguments(
    val agentId: String,
    val adminAddress: String,
    val drillInstallationDir: String = javaProcess().firstAgentPath,
    val buildVersion: String? = null,
    val instanceId: String = "",
    val groupId: String = "",
    val logLevel: String = KotlinLoggingLevel.INFO.name,
    val logFile: String? = null,
    val logLimit: Int = 512,
    val isWebApp: Boolean = false,
    val isKafka: Boolean = false,
    val isCadence: Boolean = false,
    val isTlsApp: Boolean = false,
    val isAsyncApp: Boolean = false,
    val classScanDelay: Long = 0L,
    val scanClassPath: String = "",
    val packagePrefixes: String = "",
    val sslTruststore: String = "",
    val sslTruststorePassword: String = ""
) {

    fun defaultParameters(): Map<String, AgentParameter> = mapOf(
        AgentArguments::logLevel.name to AgentParameter(
            type = logLevel.toType(),
            value = logLevel,
            description = "Logging agent work. can be TRACE|DEBUG|INFO|ERROR",
        ),
        AgentArguments::logFile.name to AgentParameter(
            type = logFile.toType(),
            value = logFile ?: "",
            description = "the location where the logs will be stored",
        ),
        AgentArguments::logLimit.name to AgentParameter(
            type = logLimit.toType(),
            value = logLimit.toString(),
            description = "the log messages max size",
        ),
        AgentArguments::isWebApp.name to AgentParameter(
            type = isWebApp.toType(),
            value = isWebApp.toString(),
            description = "",
        ),
        AgentArguments::isKafka.name to AgentParameter(
            type = isKafka.toType(),
            value = isKafka.toString(),
            description = "",
        ),
        AgentArguments::isCadence.name to AgentParameter(
            type = isCadence.toType(),
            value = isCadence.toString(),
            description = "",
        ),
        AgentArguments::isTlsApp.name to AgentParameter(
            type = isTlsApp.toType(),
            value = isTlsApp.toString(),
            description = "Add the ability of an agent to gain incoming headers from an Https request. Process TLS only for tomcat architecture",
        ),
        AgentArguments::isAsyncApp.name to AgentParameter(
            type = isAsyncApp.toType(),
            value = isAsyncApp.toString(),
            description = "",
        ),
        AgentArguments::classScanDelay.name to AgentParameter(
            type = classScanDelay.toType(),
            value = classScanDelay.toString(),
            description = "start scanning after waiting of duration in milliseconds",
        ),
        AgentArguments::scanClassPath.name to AgentParameter(
            type = scanClassPath.toType(),
            value = scanClassPath,
            description = "Add additional class path to scan",
        ),
        AgentArguments::packagePrefixes.name to AgentParameter(
            type = packagePrefixes.toType(),
            value = packagePrefixes,
            description = "Configure package prefixes for scanning and instrumentation",
        ),
        AgentArguments::sslTruststore.name to AgentParameter(
            type = sslTruststore.toType(),
            value = sslTruststore,
            description = "Configure path to SSL truststore for admin connection (leave empty to trust all)",
        ),
        AgentArguments::sslTruststorePassword.name to AgentParameter(
            type = sslTruststorePassword.toType(),
            value = sslTruststorePassword,
            description = "Configure password for SSL truststore for admin connection",
        )
    )

    private fun Any?.toType() = when (this) {
        is String, is String? -> Type.STRING.apiName
        is Boolean -> Type.BOOLEAN.apiName
        is Long -> Type.INTEGER.apiName
        else -> "Unsupported type"
    }

    private enum class Type(val apiName: String) {
        STRING("string"),
        BOOLEAN("boolean"),
        INTEGER("integer"),
    }

}
