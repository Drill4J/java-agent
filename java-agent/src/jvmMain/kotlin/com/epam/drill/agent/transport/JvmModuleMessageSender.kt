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
package com.epam.drill.agent.transport

import java.net.InetAddress
import com.epam.drill.agent.agentVersion
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val AGENT_PARAMS_FILTER = setOf("apiKey", "sslTruststorePassword", "apiUrl", "groupId", "appId", "instanceId", "commitSha", "buildVersion", "envId")

actual object JvmModuleMessageSender : AgentMessageSender by DataIngestMessageSender {
    actual fun sendAgentMetadata() {
        send(
            AgentMessageDestination("PUT", "instances"),
            InstancePayload(
                groupId = Configuration.agentMetadata.groupId,
                appId = Configuration.agentMetadata.appId,
                instanceId = Configuration.agentMetadata.instanceId,
                commitSha = Configuration.agentMetadata.commitSha,
                buildVersion = Configuration.agentMetadata.buildVersion,
                envId = Configuration.agentMetadata.envId,
                agentVersion = agentVersion,
                agentEnvironment = agentEnvironment(),
                agentParams = agentParams()
            ),
            InstancePayload.serializer()
        )
    }

    fun sendBuildMetadata() {
        send(
            AgentMessageDestination("PUT", "builds"),
            BuildPayload(
                groupId = Configuration.agentMetadata.groupId,
                appId = Configuration.agentMetadata.appId,
                commitSha = Configuration.agentMetadata.commitSha,
                buildVersion = Configuration.agentMetadata.buildVersion,
                agentVersion = agentVersion,
                agentEnvironment = agentEnvironment(),
                agentParams = agentParams()
            ),
            BuildPayload.serializer()
        )
    }

    private fun agentParams(): JsonObject = JsonObject(
        Configuration.inputParameters
            .filterKeys { it !in AGENT_PARAMS_FILTER }
            .mapValues { (_, value) -> JsonPrimitive(value) }
    )

    private fun agentEnvironment(): JsonObject = JsonObject(
        buildMap {
            put("osName", JsonPrimitive(System.getProperty("os.name")))
            put("osVersion", JsonPrimitive(System.getProperty("os.version")))
            put("osArch", JsonPrimitive(System.getProperty("os.arch")))
            put("javaVersion", JsonPrimitive(System.getProperty("java.version")))
            put("javaVendor", JsonPrimitive(System.getProperty("java.vendor")))
            put("host", JsonPrimitive(hostName()))
        }
    )

    private fun hostName(): String? = runCatching { InetAddress.getLocalHost().hostName }
        .onFailure { logger.debug(it) { "Unable to resolve local host name for agent environment." } }
        .getOrNull()
}
