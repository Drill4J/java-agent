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

import com.epam.drill.agent.instrument.*
import com.epam.drill.plugin.*
import kotlinx.serialization.protobuf.*
import java.nio.*
import mu.KotlinLogging

object HttpRequest {
    const val DRILL_HEADER_PREFIX = "drill-"

    private const val DRILL_SESSION_ID_HEADER_NAME = "${DRILL_HEADER_PREFIX}session-id"
    private const val HTTP_DETECTOR_BYTES_COUNT = 8

    private val HTTP_VERBS =
        setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI")
    private val HEADERS_END_MARK = "\r\n\r\n".encodeToByteArray()
    private val logger = KotlinLogging.logger {}

    init {
        ClientsCallback.initRequestCallback {
            loadDrillHeaders() ?: emptyMap()
        }
        ClientsCallback.initResponseCallback { headers ->
            storeDrillHeaders(headers)
        }
    }


    fun parse(buffers: Array<ByteBuffer>) = runCatching {
        val rawBytes = buffers[0].array()
        if (HTTP_VERBS.any { rawBytes.copyOf(HTTP_DETECTOR_BYTES_COUNT).decodeToString().startsWith(it) }) {
            val idx = rawBytes.indexOf(HEADERS_END_MARK)
            if (idx != -1) {
                val headers =
                    rawBytes.copyOf(idx).decodeToString().split("\r\n").drop(1).filter { it.isNotBlank() }.associate {
                        val (headerKey, headerValue) = it.split(":", limit = 2)
                        headerKey.trim() to headerValue.trim()
                    }
                //todo add processing of header mapping
                headers[DRILL_SESSION_ID_HEADER_NAME]?.let { drillSessionId ->
                    RequestHolder.storeRequest(DrillRequest(drillSessionId, headers))
                }
            }
        }
    }.onFailure { logger.error(it) { "Error while parse buffer. Reason: " } }.getOrNull()

    fun storeDrillHeaders(headers: Map<String, String>?) {
        runCatching {
            headers?.get(HeadersRetriever.sessionHeaderPattern() ?: DRILL_SESSION_ID_HEADER_NAME)?.let { drillSessionId ->
                val drillHeaders = headers.filter { it.key.startsWith(DRILL_HEADER_PREFIX) }
                logger.trace { "for drillSessionId '$drillSessionId' store drillHeaders '$drillHeaders' to thread storage" }
                RequestHolder.storeRequest(DrillRequest(drillSessionId, drillHeaders))
            }
        }.onFailure {
            logger.error(it) { "Error while storing headers. Reason: " }
        }
    }

    fun loadDrillHeaders() = runCatching {
        RequestHolder.dump()?.let { bytes ->
            val drillRequest = ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), bytes)
            drillRequest.headers.filter { it.key.startsWith(DRILL_HEADER_PREFIX) } + (DRILL_SESSION_ID_HEADER_NAME to drillRequest.drillSessionId)
        }
    }.onFailure { logger.error(it) { "Error while loading drill headers. Reason: " } }.getOrNull()

    private fun ByteArray.indexOf(arr: ByteArray) = run {
        for (index in indices) {
            if (index + arr.size <= this.size) {
                val regionMatches = arr.foldIndexed(true) { i, acc, byte ->
                    acc && this[index + i] == byte
                }
                if (regionMatches) return@run index
            } else break
        }
        -1
    }
}
