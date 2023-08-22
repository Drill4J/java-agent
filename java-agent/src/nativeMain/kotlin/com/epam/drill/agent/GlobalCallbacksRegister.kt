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
package com.epam.drill.agent

import kotlinx.serialization.protobuf.ProtoBuf
import com.epam.drill.agent.configuration.agentConfig
import com.epam.drill.agent.request.DrillRequest
import com.epam.drill.agent.request.RequestProcessor
import com.epam.drill.agent.request.RequestHolder

fun globalCallbacks(): Unit = run {
    setPackagesPrefixes = { prefixes ->
        agentConfig = agentConfig.copy(packagesPrefixes = prefixes)
    }
    sessionStorage = RequestHolder::storeRequestMetadata
    closeSession = {
        RequestHolder.closeSession()
        RequestProcessor.processServerResponse()
        RequestHolder.closeSession()
    }
    drillRequest = RequestHolder::get
}

private fun RequestHolder.storeRequestMetadata(request: DrillRequest) {
    store(ProtoBuf.encodeToByteArray(DrillRequest.serializer(), request))
}

private fun RequestHolder.get(): DrillRequest? {
    return dump()?.let { ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), it) }
}
