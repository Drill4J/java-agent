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
package com.epam.drill.agent.request

import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import com.alibaba.ttl.TransmittableThreadLocal
import com.epam.drill.agent.RequestAgentContext
import com.epam.drill.common.agent.AgentContext
import com.epam.drill.plugin.DrillRequest

actual object RequestHolder {

    private val logger = KotlinLogging.logger {}

    private lateinit var threadStorage: InheritableThreadLocal<DrillRequest>

    val agentContext: AgentContext = RequestAgentContext { threadStorage.get() }

    actual fun init(isAsync: Boolean) {
        threadStorage = if (isAsync) TransmittableThreadLocal() else InheritableThreadLocal()
    }

    actual fun store(drillRequest: ByteArray) {
        storeRequest(ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), drillRequest))
    }

    fun storeRequest(drillRequest: DrillRequest) {
        threadStorage.set(drillRequest)
        logger.trace { "session ${drillRequest.drillSessionId} saved" }
        RequestProcessor.processServerRequest()
    }

    actual fun dump(): ByteArray? {
        return threadStorage.get()?.let { ProtoBuf.encodeToByteArray(DrillRequest.serializer(), it) }
    }

    actual fun closeSession() {
        logger.trace { "session ${threadStorage.get()} closed" }
        threadStorage.remove()
    }

}
