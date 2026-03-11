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
package com.epam.drill.agent.instrument

import java.util.Objects
import mu.KLogger
import mu.KotlinLogging
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.HeadersRetriever
import com.epam.drill.agent.common.request.RequestHolder

open class DrillRequestHeadersProcessor(
    private val headersRetriever: HeadersRetriever,
    private val requestHolder: RequestHolder
) : HeadersProcessor {

    private val logger: KLogger = KotlinLogging.logger {}

    override fun removeHeaders() = requestHolder.remove()

    override fun storeHeaders(headers: Map<String, String>) {
        try {
            logger.trace { "storeHeaders: Unfiltered headers: $headers" }

            val sessionHeaderName = headersRetriever.sessionHeader()
            val sessionIdFromHeader = headers[sessionHeaderName]

            if (sessionIdFromHeader != null) {
                val filtered = headers
                    .filterKeys(Objects::nonNull)
                    .filter { it.key.startsWith(HeadersProcessor.DRILL_HEADER_PREFIX) }
                logger.trace { "storeHeaders: from headers, sessionId=$sessionIdFromHeader: $filtered" }
                requestHolder.store(DrillRequest(sessionIdFromHeader, filtered))
            } else {
                val cookieHeader = headers.entries
                    .firstOrNull { it.key.equals("Cookie", ignoreCase = true) }
                    ?.value
                val cookieMap = cookieHeader
                    ?.split(";")
                    ?.mapNotNull {
                        val parts = it.trim().split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }
                    ?.toMap()
                    ?: emptyMap()

                val sessionIdFromCookie = cookieMap["drill-session-id"]
                if (sessionIdFromCookie != null) {
                    val filtered = cookieMap
                        .filterKeys { it.startsWith(HeadersProcessor.DRILL_HEADER_PREFIX) }
                    logger.trace { "storeHeaders: from cookies, sessionId=$sessionIdFromCookie: $filtered" }

                    requestHolder.store(DrillRequest(sessionIdFromCookie, filtered))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "storeHeaders: Error while storing headers" }
        }
    }

    override fun retrieveHeaders() = try {
        requestHolder.retrieve()?.let { drillRequest ->
            logger.trace { "retrieveHeaders: Raw DrillRequest headers: ${drillRequest.headers}" }

            val filtered = drillRequest.headers
                .filter { it.key.startsWith(HeadersProcessor.DRILL_HEADER_PREFIX) }

            val result = filtered + (headersRetriever.sessionHeader() to drillRequest.drillSessionId)

            logger.trace { "retrieveHeaders: Returning headers, sessionId=${drillRequest.drillSessionId}: $result" }

            result
        }
    } catch (e: Exception) {
        logger.error(e) { "retrieveHeaders: Error while loading drill headers" }
        null
    }

    override fun hasHeaders() = requestHolder.retrieve() != null

    override fun isProcessRequests() = true

    override fun isProcessResponses() = true

}
