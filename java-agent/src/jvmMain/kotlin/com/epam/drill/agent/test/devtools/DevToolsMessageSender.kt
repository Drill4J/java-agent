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
package com.epam.drill.agent.test.devtools

import mu.KotlinLogging
import com.epam.drill.agent.transport.JsonAgentMessageSerializer
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.ResponseStatus
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.instrument.selenium.DevToolsMessage
import com.epam.drill.agent.transport.JsonAgentMessageDeserializer
import kotlin.reflect.KClass

object DevToolsMessageSender {

    private val messageTransport = HttpAgentMessageTransport(
        serverAddress = Configuration.parameters[ParameterDefinitions.DEVTOOLS_PROXY_ADDRESS],
        drillInternal = false,
        gzipCompression = false,
    )
    private val messageSerializer = JsonAgentMessageSerializer()
    private val messageDeserializer = JsonAgentMessageDeserializer()
    private val logger = KotlinLogging.logger {}

    fun send(
        method: String,
        path: String,
        message: DevToolsMessage
    ) = messageTransport.send(
        AgentMessageDestination(method, path),
        messageSerializer.serialize(message, DevToolsMessage.serializer()),
        messageSerializer.contentType()
    )
        .mapContent { it.decodeToString() }
        .also(DevToolsMessageSender::logResponseContent)

    fun <T : AgentMessage> send(
        method: String,
        path: String,
        message: DevToolsMessage,
        clazz: KClass<T>
    ) = messageTransport.send(
        AgentMessageDestination(method, path),
        messageSerializer.serialize(message, DevToolsMessage.serializer()),
        messageSerializer.contentType()
    )
        .mapContent {
            messageDeserializer.deserialize(it, clazz)
        }
        .also(DevToolsMessageSender::logResponseContent)

    fun send(
        serverAddress: String,
        method: String,
        path: String,
        message: String
    ): ResponseStatus<String> = HttpAgentMessageTransport(
        serverAddress = serverAddress,
        drillInternal = false,
        gzipCompression = false,
    ).send(
        AgentMessageDestination(method, path),
        message.encodeToByteArray(),
        messageSerializer.contentType()
    )
        .mapContent { it.decodeToString() }
        .also(DevToolsMessageSender::logResponseContent)

    private fun logResponseContent(responseContent: ResponseStatus<*>) = logger.trace {
        val response = responseContent.content.toString()
            .takeIf(String::isNotEmpty)
            ?.let { "\n${it.prependIndent("\t")}" }
            ?: " <empty>"
        "send: Response received, success=${responseContent.success}: $response"
    }

}
