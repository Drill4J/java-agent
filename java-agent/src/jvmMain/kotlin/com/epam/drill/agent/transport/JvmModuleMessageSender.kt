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

import java.io.File
import io.aesy.datasize.ByteUnit
import io.aesy.datasize.DataSize
import mu.KotlinLogging
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.transport.http.HttpAgentMessageDestinationMapper
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender

actual object JvmModuleMessageSender : AgentMessageSender {

    private const val QUEUE_DEFAULT_SIZE: Long = 512L * 1024 * 1024

    private val logger = KotlinLogging.logger {}
    private val messageSender = messageSender()

    override val available: Boolean
        get() = messageSender.available

    override fun send(destination: AgentMessageDestination, message: AgentMessage) =
        messageSender.send(destination, message)

    actual fun sendAgentMetadata() {
        messageSender.send(Configuration.agentMetadata)
    }

    private fun messageSender(): QueuedAgentMessageSender<ByteArray> {
        val transport = HttpAgentMessageTransport(
            Configuration.parameters[ParameterDefinitions.ADMIN_ADDRESS],
            Configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE].let(::resolvePath),
            Configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE_PASSWORD]
        )
        val serializer = ProtoBufAgentMessageSerializer()
        val mapper = HttpAgentMessageDestinationMapper(
            Configuration.agentMetadata.id,
            Configuration.agentMetadata.buildVersion
        )
        val metadataSender = RetryingAgentMetadataSender(transport, serializer, mapper)
        val queue = InMemoryAgentMessageQueue(
            serializer,
            Configuration.parameters[ParameterDefinitions.MESSAGE_QUEUE_LIMIT].let(::parseBytes)
        )
        val notifier = RetryingTransportStateNotifier(transport, serializer, queue)
        return QueuedAgentMessageSender(transport, serializer, mapper, metadataSender, notifier, notifier, queue)
    }

    private fun resolvePath(path: String) = File(path).run {
        val installationDir = File(Configuration.parameters[DefaultParameterDefinitions.INSTALLATION_DIR])
        val resolved = this.takeIf(File::exists)
            ?: this.takeUnless(File::isAbsolute)?.let(installationDir::resolve)
        logger.trace { "resolvePath: Resolved $path to ${resolved?.absolutePath}" }
        resolved?.absolutePath ?: path
    }

    private fun parseBytes(value: String): Long = value.run {
        val logError: (Throwable) -> Unit = { logger.warn(it) { "parseBytes: Exception while parsing value: $this" } }
        this.runCatching(DataSize::parse)
            .onFailure(logError)
            .getOrDefault(DataSize.of(QUEUE_DEFAULT_SIZE, ByteUnit.BYTE))
            .toUnit(ByteUnit.BYTE).value.toLong()
    }

}
