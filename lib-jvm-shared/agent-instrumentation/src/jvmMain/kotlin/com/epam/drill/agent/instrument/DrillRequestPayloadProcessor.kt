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

open class DrillRequestPayloadProcessor(
    private val enabled: () -> Boolean = { true },
    private val headersProcessor: HeadersProcessor
) : PayloadProcessor {

    private val payloadPrefixBytes = PayloadProcessor.PAYLOAD_PREFIX.encodeToByteArray()

    override fun retrieveDrillHeaders(message: String) = message.takeIf { it.endsWith(PayloadProcessor.PAYLOAD_SUFFIX) }
        ?.removeSuffix(PayloadProcessor.PAYLOAD_SUFFIX)
        ?.substringAfter(PayloadProcessor.PAYLOAD_PREFIX)
        ?.split("\n")
        ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        ?.also(headersProcessor::storeHeaders)
        ?.let { message.substringBefore(PayloadProcessor.PAYLOAD_PREFIX) }
        ?: message

    override fun retrieveDrillHeaders(message: ByteArray) =
        retrieveDrillHeaders(message.decodeToString()).encodeToByteArray()

    override fun retrieveDrillHeadersIndex(message: ByteArray) = message.decodeToString()
        .takeIf { it.endsWith(PayloadProcessor.PAYLOAD_SUFFIX) }
        ?.removeSuffix(PayloadProcessor.PAYLOAD_SUFFIX)
        ?.substringAfter(PayloadProcessor.PAYLOAD_PREFIX)
        ?.split("\n")
        ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
        ?.also(headersProcessor::storeHeaders)
        ?.let { drillPayloadBytesIndex(message) }

    override fun storeDrillHeaders(message: String?) = message
        ?.let { headersProcessor.retrieveHeaders() }
        ?.map { (k, v) -> "$k=$v" }
        ?.joinToString("\n", PayloadProcessor.PAYLOAD_PREFIX, PayloadProcessor.PAYLOAD_SUFFIX)
        ?.let(message::plus)

    override fun storeDrillHeaders(message: ByteArray?) = message
        ?.let { storeDrillHeaders(message.decodeToString())!!.encodeToByteArray() }

    override fun isPayloadProcessingEnabled() = enabled()

    override fun isPayloadProcessingSupported(headers: Map<String, String>?) =
        headers != null
                && headers.containsKey(PayloadProcessor.HEADER_WS_PER_MESSAGE)
                && headers[PayloadProcessor.HEADER_WS_PER_MESSAGE].toBoolean()

    private fun drillPayloadBytesIndex(bytes: ByteArray): Int {
        for (currentIndex in IntRange(0, bytes.lastIndex - payloadPrefixBytes.lastIndex)) {
            val regionMatches = payloadPrefixBytes.foldIndexed(true) { index, acc, byte ->
                acc && bytes[currentIndex + index] == byte
            }
            if (regionMatches) return currentIndex
        }
        return -1
    }

}
