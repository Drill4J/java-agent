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
package com.epam.drill.agent.test.instrument.selenium

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import com.epam.drill.agent.common.transport.AgentMessage

@Serializable
data class TargetInfos(val targetInfos: List<Target>): AgentMessage()

@Serializable
data class SessionId(val sessionId: String = ""): AgentMessage()

@Serializable
data class Target(
    val targetId: String,
    val type: String,
    val title: String,
    val url: String,
    val attached: Boolean,
    val browserContextId: String,
)

@Serializable
sealed class DevToolsMessage : AgentMessage() {
    abstract val target: String
}

@Serializable
data class DevToolsRequest(
    override val target: String,
    val sessionId: String = "",
    val params: Map<String, JsonElement> = emptyMap()
) : DevToolsMessage()

@Serializable
data class DevToolInterceptRequest(
    override val target: String,
    val params: Map<String, Map<String, String>> = emptyMap()
) : DevToolsMessage()

@Serializable
data class DevToolsHeaderRequest(
    override val target: String,
    val sessionId: String,
    val params: Map<String, Map<String, String>> = emptyMap(),
) : DevToolsMessage()
