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

interface PayloadProcessor {
    companion object {
        const val HEADER_WS_PER_MESSAGE = "drill-ws-per-message"
        const val PAYLOAD_PREFIX = "\n\ndrill-payload-begin\n"
        const val PAYLOAD_SUFFIX = "\ndrill-payload-end"
    }
    fun retrieveDrillHeaders(message: String): String
    fun retrieveDrillHeaders(message: ByteArray): ByteArray
    fun retrieveDrillHeadersIndex(message: ByteArray): Int?
    fun storeDrillHeaders(message: String?): String?
    fun storeDrillHeaders(message: ByteArray?): ByteArray?
    fun isPayloadProcessingEnabled(): Boolean
    fun isPayloadProcessingSupported(headers: Map<String, String>?): Boolean
}
