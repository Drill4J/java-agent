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

import com.epam.drill.common.agent.request.DrillRequest

@ThreadLocal
private var localRequest: DrillRequest? = null

class HttpInterceptorCallbacks(
    private val idHeaderPair: Pair<String, String>,
    private val adminAddressPair: Pair<String, String>,
    private val requestPattern: String,
    private val drillRequest: () -> DrillRequest?,
    private val sessionStorage: (DrillRequest) -> Unit,
    private val closeSession: () -> Unit
) {

    fun readHeaders(headers: Map<ByteArray, ByteArray>) {
        val decoded = headers.entries.associate { (k, v) -> k.decodeToString().lowercase() to v.decodeToString() }
        decoded[requestPattern]?.also {
            val request = DrillRequest(it, decoded)
            localRequest = request
            sessionStorage(request)
        }
    }

    @Suppress("unused_parameter")
    fun writeCallback(bytes: ByteArray) {
        closeSession()
        localRequest = null
    }

    fun injectedHeaders(): Map<String, String> {
        val existing = drillRequest()?.headers
            ?.filterKeys { it.startsWith("drill-") }
            ?: emptyMap()
        return existing + mapOf(idHeaderPair, adminAddressPair)
    }

}
