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

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageReceiver
import kotlin.reflect.KClass

class SimpleAgentMessageReceiver(
    private val transport: AgentMessageTransport,
    private val messageDeserializer: AgentMessageDeserializer,
    private val destinationMapper: AgentMessageDestinationMapper = StubAgentDestinationMapper
) : AgentMessageReceiver {

    override fun <T : Any> receive(destination: AgentMessageDestination, clazz: KClass<T>): T =
        transport.send(destinationMapper.map(destination), null)
            .mapContent { messageDeserializer.deserialize(it, clazz) }
            .onError {
                error("Failed to receive message from $destination, error message: $it")
            }.content
            ?: error("Failed to receive message from $destination. There is no content in the response.")
}