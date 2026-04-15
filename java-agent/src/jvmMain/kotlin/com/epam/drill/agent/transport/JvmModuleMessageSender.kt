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

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender

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
                envId = Configuration.agentMetadata.envId
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
                buildVersion = Configuration.agentMetadata.buildVersion
            ),
            BuildPayload.serializer()
        )
    }
}
