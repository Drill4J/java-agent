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

import com.epam.drill.agent.common.transport.AgentMessage
import kotlinx.serialization.KSerializer

/**
 * A message serializer interface for [AgentMessage] serialization before sending by [AgentMessageTransport].
 *
 * It's used to serialize [AgentMessage] before storing in [AgentMessageQueue] or sending by [AgentMessageTransport].
 * It contains [contentType] and [sizeOf] functions that can be used in [AgentMessageQueue] and [AgentMessageTransport]
 * to abstract from serialized data details.
 *
 * @see AgentMessage
 * @see AgentMessageQueue
 * @see AgentMessageTransport
 */
interface AgentMessageSerializer {
    fun contentType(): String
    fun <T> serialize(message: T, serializer: KSerializer<T>): ByteArray
}
