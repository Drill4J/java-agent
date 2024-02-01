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
package com.epam.drill.agent

import com.epam.drill.agent.configuration.TransportConfiguration
import com.epam.drill.agent.transport.InMemoryAgentMessageQueue
import com.epam.drill.agent.transport.ProtoBufAgentMessageSerializer
import com.epam.drill.agent.transport.QueuedAgentMessageSender
import com.epam.drill.agent.transport.RetryingAgentConfigSender
import com.epam.drill.agent.transport.RetryingTransportStateNotifier
import com.epam.drill.agent.transport.http.HttpAgentMessageDestinationMapper
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport

actual object JvmModuleMessageSender : QueuedAgentMessageSender<ByteArray>(
    transport,
    serializer,
    mapper,
    configSender,
    stateNotifier,
    stateNotifier,
    queue
) {
    actual fun sendAgentConfig() = send(TransportConfiguration.getAgentConfigBytes()).let {}
}

private val transport = HttpAgentMessageTransport(
    TransportConfiguration.getAdminAddress(),
    TransportConfiguration.getSslTruststoreResolved(),
    TransportConfiguration.getSslTruststorePassword(),
    TransportConfiguration.getApiKey()
)

private val serializer = ProtoBufAgentMessageSerializer()

private val mapper = HttpAgentMessageDestinationMapper(
    TransportConfiguration.getAgentId(),
    TransportConfiguration.getBuildVersion()
)

private val configSender = RetryingAgentConfigSender(transport, serializer, mapper)

private val queue = InMemoryAgentMessageQueue(serializer, TransportConfiguration.getCoverageRetentionLimitBytes())

private val stateNotifier = RetryingTransportStateNotifier(transport, serializer, queue)
