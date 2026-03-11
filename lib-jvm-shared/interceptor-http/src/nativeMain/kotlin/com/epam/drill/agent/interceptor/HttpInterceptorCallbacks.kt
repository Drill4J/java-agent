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
package com.epam.drill.agent.interceptor

import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.HeadersRetriever
import com.epam.drill.agent.common.request.RequestHolder

class HttpInterceptorCallbacks(
    private val headersRetriever: HeadersRetriever,
    private val requestHolder: RequestHolder
) {

    private val agentIdPair = headersRetriever.agentIdHeader() to headersRetriever.agentIdHeaderValue()
    private val adminAddressPair = headersRetriever.adminAddressHeader() to headersRetriever.adminAddressValue()

    fun readHeaders(headers: Map<ByteArray, ByteArray>) {
        val decoded = headers.entries.associate { (k, v) -> k.decodeToString() to v.decodeToString() }
        decoded[headersRetriever.sessionHeader()]?.also { requestHolder.store(DrillRequest(it, decoded)) }
    }

    @Suppress("unused_parameter")
    fun writeCallback(bytes: ByteArray) {
        requestHolder.remove()
    }

    fun injectedHeaders(): Map<String, String> {
        val existing = requestHolder.retrieve()?.headers
            ?.filterKeys { it.startsWith("drill-") }
            ?: emptyMap()
        return existing + mapOf(agentIdPair, adminAddressPair)
    }

}
