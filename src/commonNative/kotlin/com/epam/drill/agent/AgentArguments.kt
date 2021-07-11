/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent

import com.benasher44.uuid.*
import com.epam.drill.common.*
import com.epam.drill.logger.api.*
import kotlinx.serialization.*

@Serializable
data class AgentArguments(
    val agentId: String,
    val adminAddress: String,
    val drillInstallationDir: String = javaProcess().firstAgentPath,
    val buildVersion: String? = null,
    val instanceId: String = uuid4().toString(),
    val groupId: String = "",
    val logLevel: String = LogLevel.ERROR.name,
    val logFile: String? = null,
    val isWebApp: Boolean = false,
    val isKafka: Boolean = false,
    val isTlsApp: Boolean = false,
    val isAsyncApp: Boolean = false,
    val webAppNames: String = "",
    val classScanDelay: Long = 0L,
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
        AgentArguments::isTlsApp.name to AgentParameter(
            type = isTlsApp.toType(),
            value = isTlsApp.toString(),
            description = "Add the ability of an agent to gain incoming headers from an Https request." +
                    "Process TLS only for tomcat architecture",
        ),
        AgentArguments::isAsyncApp.name to AgentParameter(
            type = isAsyncApp.toType(),
            value = isAsyncApp.toString(),
            description = "",
        ),
        AgentArguments::webAppNames.name to AgentParameter(
            type = webAppNames.toType(),
            value = webAppNames,
            description = "",
        ),
        AgentArguments::classScanDelay.name to AgentParameter(
            type = classScanDelay.toType(),
            value = classScanDelay.toString(),
            description = "start scanning after waiting of duration in milliseconds",
        ),
    )

}

fun Any?.toType() = when (this) {
    is String, is String? -> Type.STRING.apiName
    is Boolean -> Type.BOOLEAN.apiName
    is Long -> Type.INTEGER.apiName
    else -> "Unsupported type"
}

enum class Type(val apiName: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
}
