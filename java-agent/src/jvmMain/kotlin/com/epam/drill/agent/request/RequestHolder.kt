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
import com.alibaba.ttl.TransmittableThreadLocal
import mu.KotlinLogging
import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.common.agent.request.RequestHolder

actual object RequestHolder : RequestHolder {

    private val logger = KotlinLogging.logger {}
    private lateinit var threadStorage: InheritableThreadLocal<DrillRequest>

    actual override fun remove() {
        RequestProcessor.processServerResponse()
        logger.trace { "remove: Request ${threadStorage.get()} removed" }
        threadStorage.remove()
    }

    actual override fun retrieve(): DrillRequest? =
        threadStorage.get()

    actual override fun store(drillRequest: DrillRequest){
        threadStorage.set(drillRequest)
        logger.trace { "store: Request ${drillRequest.drillSessionId} saved" }
        RequestProcessor.processServerRequest()
    }

    actual fun store(drillRequest: ByteArray) =
        store(ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), drillRequest))

    actual fun dump(): ByteArray? =
        threadStorage.get()?.let { ProtoBuf.encodeToByteArray(DrillRequest.serializer(), it) }

    actual operator fun invoke(isAsync: Boolean) {
        threadStorage = if (isAsync) TransmittableThreadLocal() else InheritableThreadLocal()
    }

}
