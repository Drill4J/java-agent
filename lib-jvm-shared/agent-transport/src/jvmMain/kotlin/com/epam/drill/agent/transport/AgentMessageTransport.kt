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
import com.epam.drill.agent.common.transport.ResponseStatus

/**
 * A transport interface for serialized messages.
 *
 * It's used to send serialized [com.epam.drill.agent.common.transport.AgentMessage]
 * to transport-specific [AgentMessageDestination]. Serialization and destination mapping should be done
 * by [AgentMessageSerializer] and [AgentMessageDestinationMapper] correspondingly.
 * In case of transport errors messages may be stored in [AgentMessageQueue] for subsequent retries.
 *
 * @see AgentMessageDestination
 * @see AgentMessageSerializer
 * @see AgentMessageDestinationMapper
 * @see AgentMessageQueue
 * @see com.epam.drill.agent.common.transport.AgentMessage
 */
interface AgentMessageTransport {
    fun send(destination: AgentMessageDestination, message: ByteArray? = null, contentType: String = ""): ResponseStatus<ByteArray>
}
