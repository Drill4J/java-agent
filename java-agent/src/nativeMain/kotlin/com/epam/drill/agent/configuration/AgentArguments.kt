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
import com.epam.drill.common.agent.configuration.AgentParameter

@Serializable
data class AgentArguments(
    val drillInstallationDir: String? = null,
    var adminAddress: String? = null,
    var packagePrefixes: String? = null,
    var agentId: String? = null,
    var buildVersion: String? = null,
    var groupId: String = "",
    var instanceId: String = "",
    var logLevel: String = KotlinLoggingLevel.INFO.name,
    var logFile: String = "",
    var logLimit: Int = 512,
    var isWebApp: Boolean = false,
    var isKafka: Boolean = false,
    var isCadence: Boolean = false,
    var isTlsApp: Boolean = false,
    var isAsyncApp: Boolean = false,
    var classScanDelay: Long = 0L,
    var scanClassPath: String = "",
    var sslTruststore: String = "",
    var sslTruststorePassword: String = "",
    val coverageRetentionLimit: String = "512Mb",
    val sendCoverageIntervalMs: Long = 2000L
) {

    val packagePrefixesToList: List<String>
        get() = packagePrefixes?.split(";")?.toList() ?: emptyList()

    val scanClassPathToList: List<String>
        get() = if (scanClassPath.isEmpty()) emptyList() else scanClassPath.split(";").toList()

    val logLevelToList: List<String>
        get() = logLevel.split(";").toList()

    fun defaultParameters(): Map<String, AgentParameter> = mapOf(
        AgentArguments::logLevel.name to AgentParameter(
            type = logLevel.toType(),
            value = logLevel,
            description = "Logging agent work. can be TRACE|DEBUG|INFO|ERROR",
        ),
        AgentArguments::logFile.name to AgentParameter(
            type = logFile.toType(),
            value = logFile,
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
            value = packagePrefixes!!,
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
        ),
        AgentArguments::coverageRetentionLimit.name to AgentParameter(
            type = coverageRetentionLimit.toType(),
            value = coverageRetentionLimit,
            description = "Coverage retention queue size limit. Defaults to default value. Accepts human-readable data size format, e.g.: \"512Mb\",\"0.5 Gb\", \"1Gb\""
        ),
        AgentArguments::sendCoverageIntervalMs.name to AgentParameter(
            type = sendCoverageIntervalMs.toType(),
            value = sendCoverageIntervalMs.toString(),
            description = "Coverage sending interval in milliseconds"
        ),
        AgentArguments::drillInstallationDir.name to AgentParameter(
            type = drillInstallationDir.toType(),
            value = drillInstallationDir.toString(),
            description = "Drill4J java agent installation directory"
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
