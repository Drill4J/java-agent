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
package com.epam.drill.agent.configuration

import kotlin.native.concurrent.*
import mu.*
import com.epam.drill.agent.*
import com.epam.drill.agent.request.*
import com.epam.drill.hook.io.*
import com.epam.drill.interceptor.*

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.ConfigureHttp")

@ThreadLocal
internal var drillRequest: DrillRequest? = null

fun configureHttp() {
    configureHttpInterceptor()
    injectedHeaders.value = {
        val idHeaderPair = HeadersRetriever.idHeaderPair
        val adminUrl = HeadersRetriever.adminAddress
        mapOf(
            idHeaderPair,
            "drill-admin-url" to adminUrl
        ) + (drillRequest()?.headers?.filterKeys { it.startsWith("drill-") } ?: mapOf())

    }.freeze()
    readHeaders.value = { rawHeaders: Map<ByteArray, ByteArray> ->
        val headers = rawHeaders.entries.associate { (k, v) ->
            k.decodeToString().lowercase() to v.decodeToString()
        }
        if (KotlinLoggingLevel.DEBUG.isLoggingEnabled()) {
            val drillHeaders = headers.filterKeys { it.startsWith("drill-") }
            if (drillHeaders.any()) {
                logger.debug { "Drill headers: $drillHeaders" }
            }
        }
        val sessionId = headers[HeadersRetriever.requestPattern]
        sessionId?.let { DrillRequest(it, headers) }?.also {
            drillRequest = it
            sessionStorage(it)
        }
        Unit
    }.freeze()
    writeCallback.value = { _: ByteArray ->
        closeSession()
        drillRequest = null
    }.freeze()

}
