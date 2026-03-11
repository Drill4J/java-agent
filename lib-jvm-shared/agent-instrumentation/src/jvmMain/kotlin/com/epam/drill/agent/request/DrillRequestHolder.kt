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

import com.epam.drill.agent.ttl.TransmittableThreadLocal
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.DrillInitialContext
import com.epam.drill.agent.common.request.RequestHolder
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging

actual object DrillRequestHolder : RequestHolder {
    private val logger = KotlinLogging.logger {}
    private var threadStorage: ThreadLocal<DrillRequest> = TransmittableThreadLocal.withInitial(DrillInitialContext::getDrillRequest)

    actual override fun remove() {
        val request = threadStorage.get()
        if (request == null) return
        DrillRequestProcessor.processServerResponse()
        threadStorage.remove()
        logger.trace { "remove: Request ${request.drillSessionId} removed, threadId = ${Thread.currentThread().id}" }
    }

    actual override fun retrieve(): DrillRequest? =
        threadStorage.get()

    actual override fun store(drillRequest: DrillRequest) {
        remove()
        threadStorage.set(drillRequest)
        DrillRequestProcessor.processServerRequest()
        logger.trace { "store: Request ${drillRequest.drillSessionId} saved, threadId = ${Thread.currentThread().id}" }
    }

    actual fun store(drillRequest: ByteArray) =
        store(ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), drillRequest))

    actual fun dump(): ByteArray? =
        retrieve()?.let { ProtoBuf.encodeToByteArray(DrillRequest.serializer(), it) }

}