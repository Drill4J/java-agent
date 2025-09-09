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

import com.epam.drill.agent.common.configuration.AgentMetadata
import java.io.File
import io.aesy.datasize.ByteUnit
import io.aesy.datasize.DataSize
import mu.KotlinLogging
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer

actual object JvmModuleMessageSender : AgentMessageSender {

    private const val QUEUE_DEFAULT_SIZE: Long = 512L * 1024 * 1024

    private val logger = KotlinLogging.logger {}
    private val messageSender = messageSender()

    override fun <T> send(destination: AgentMessageDestination, message: T, serializer: KSerializer<T>) =
        messageSender.send(destination, message, serializer)

    actual fun sendAgentMetadata() {
        messageSender.send(AgentMessageDestination("PUT", "instances"), Configuration.agentMetadata, AgentMetadata.serializer())
    }

    override fun shutdown() {
        messageSender.shutdown()
    }

    @OptIn(InternalSerializationApi::class)
    private fun messageSender(): QueuedAgentMessageSender {
        val transport = HttpAgentMessageTransport(
            serverAddress = Configuration.parameters[ParameterDefinitions.API_URL],
            apiKey = Configuration.parameters[ParameterDefinitions.API_KEY] ?: "",
            sslTruststore = Configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE]
                ?.let(::resolvePath) ?: "",
            sslTruststorePass = Configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE_PASSWORD] ?: "",
            gzipCompression = Configuration.parameters[ParameterDefinitions.USE_GZIP_COMPRESSION],
        )
        val serializer = takeIf { Configuration.parameters[ParameterDefinitions.USE_PROTOBUF_SERIALIZER] }?.let {
            ProtoBufAgentMessageSerializer()
        } ?: JsonAgentMessageSerializer()
        val mapper = HttpAgentMessageDestinationMapper()
        val queue = InMemoryAgentMessageQueue(
            capacity = Configuration.parameters[ParameterDefinitions.MESSAGE_QUEUE_LIMIT].let(::parseBytes),
        )
        return QueuedAgentMessageSender(
            transport, serializer, mapper, queue,
            maxRetries = Configuration.parameters[ParameterDefinitions.MESSAGE_MAX_RETRIES]
        )
    }

    private fun resolvePath(path: String) = File(path).run {
        val installationDir = File(Configuration.parameters[DefaultParameterDefinitions.INSTALLATION_DIR])
        val resolved = this.takeIf(File::exists)
            ?: this.takeUnless(File::isAbsolute)?.let(installationDir::resolve)
        logger.trace { "resolvePath: Resolved $path to ${resolved?.absolutePath}" }
        resolved?.takeUnless(File::isDirectory)?.absolutePath ?: path
    }

    private fun parseBytes(value: String): Long = value.run {
        val logError: (Throwable) -> Unit = { logger.warn(it) { "parseBytes: Exception while parsing value: $this" } }
        this.runCatching(DataSize::parse)
            .onFailure(logError)
            .getOrDefault(DataSize.of(QUEUE_DEFAULT_SIZE, ByteUnit.BYTE))
            .toUnit(ByteUnit.BYTE).value.toLong()
    }

}
