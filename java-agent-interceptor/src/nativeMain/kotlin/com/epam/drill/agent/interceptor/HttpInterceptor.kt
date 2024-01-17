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

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import mu.KotlinLogging
import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.io.CR_LF
import com.epam.drill.hook.io.CR_LF_BYTES
import com.epam.drill.hook.io.HEADERS_DELIMITER
import com.epam.drill.hook.io.Interceptor
import com.epam.drill.hook.io.TcpFinalData
import com.epam.drill.hook.io.injectedHeaders
import com.epam.drill.hook.io.readCallback
import com.epam.drill.hook.io.readHeaders
import com.epam.drill.hook.io.writeCallback

private const val HTTP_DETECTOR_BYTES_COUNT = 8
private const val HTTP_RESPONSE_MARKER = "HTTP"
private const val HTTP_HEADER_DRILL_INTERNAL = "drill-internal: true"

@ThreadLocal
private val localRequestBytes = mutableMapOf<DRILL_SOCKET, ByteArray?>()

class HttpInterceptor : Interceptor {

    private val httpVerbs = setOf(
        "OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI", HTTP_RESPONSE_MARKER
    )
    private val drillInternalHeader = HTTP_HEADER_DRILL_INTERNAL.encodeToByteArray()
    private val logger = KotlinLogging.logger("com.epam.drill.agent.interceptor.HttpInterceptor")

    override fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) = try {
        val prefix = bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString()
        val readBytes by lazy { bytes.readBytes(size.convert()) }
        when {
            httpVerbs.any(prefix::startsWith) && !containsDrillInternalHeader(readBytes) -> {
                if (containsFullHeadersPart(readBytes)) readHeaders(fd, readBytes)
                else localRequestBytes[fd] = readBytes
            }
            localRequestBytes[fd] != null -> {
                val total = localRequestBytes[fd]!! + readBytes
                if (containsFullHeadersPart(readBytes)) readHeaders(fd, total).also { localRequestBytes.remove(fd) }
                else localRequestBytes[fd] = total
            }
            else -> Unit
        }
    } catch (e: Exception) {
        logger.error(e) { "interceptRead: $e" }
    }

    override fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) = try {
        val prefix = bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString()
        val readBytes by lazy { bytes.readBytes(size.convert()) }
        val headersSeparator by lazy { readBytes.indexOf(CR_LF_BYTES) }
        val writeHeaders by lazy { injectedHeaders.value() }
        when {
            prefix.startsWith("PRI") -> {
                TcpFinalData(bytes, size)
            }
            headersSeparator > 0 && containsDrillInternalHeader(readBytes) -> {
                TcpFinalData(bytes, size)
            }
            headersSeparator > 0 && !containsHeaders(readBytes, writeHeaders) -> {
                val modified = writeHeaders(fd, readBytes, headersSeparator, writeHeaders)
                TcpFinalData(modified.toCValues().getPointer(this), modified.size, modified.size - size)
            }
            else -> {
                TcpFinalData(bytes, size)
            }
        }
    } catch (e: Exception) {
        logger.error(e) { "interceptWrite: $e" }
        TcpFinalData(bytes, size)
    }

    override fun close(fd: DRILL_SOCKET) {
        localRequestBytes.remove(fd)
    }

    override fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>) =
        bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString().let { httpVerbs.any(it::startsWith) }

    private fun readHeaders(fd: DRILL_SOCKET, bytes: ByteArray) {
        val decoded = bytes.decodeToString()
        logger.trace { "readHeaders: Reading HTTP request from fd=$fd:\n${decoded.prependIndent("\t")}" }
        readHeaders.value(
            decoded.subSequence(decoded.indexOf('\r'), decoded.indexOf(CR_LF + CR_LF))
                .split(CR_LF)
                .filter(String::isNotBlank)
                .map { it.split(":", limit = 2).map(String::trim) }
                .onEach { logger.trace { "readHeaders: Read HTTP header from fd=$fd: ${it[0]}=${it[1]}" } }
                .associate { it[0].encodeToByteArray() to it[1].encodeToByteArray() }
        )
        readCallback.value(bytes)
    }

    private fun writeHeaders(fd: DRILL_SOCKET, bytes: ByteArray, index: Int, headers: Map<String, String>): ByteArray {
        headers.entries.forEach { logger.trace { "writeHeaders: Writing HTTP header to fd=$fd: ${it.key}=${it.value}" } }
        val responseHead = bytes.copyOfRange(0, index)
        val injectedHeaders = headersToBytes(headers)
        val responseTail = bytes.copyOfRange(index, bytes.size)
        val modified = responseHead + injectedHeaders + responseTail
        logger.trace {
            val headersRange = modified.indexOf(HEADERS_DELIMITER).takeUnless((-1)::equals) ?: bytes.size
            val headersPart = modified.copyOfRange(0, headersRange).decodeToString()
            "writeHeaders: Written HTTP headers to fd=$fd: \n${headersPart.prependIndent("\t")}"
        }
        writeCallback.value(modified)
        return modified
    }

    private fun containsDrillInternalHeader(bytes: ByteArray) = bytes.indexOf(drillInternalHeader) != -1

    private fun containsFullHeadersPart(bytes: ByteArray) = bytes.indexOf(HEADERS_DELIMITER) != -1

    private fun containsHeaders(bytes: ByteArray, headers: Map<String, String>) =
        headers.isNotEmpty() && bytes.indexOf(headers.keys.first().encodeToByteArray()) != -1

    private fun headersToBytes(headers: Map<String, String>) = headers.map { (k, v) -> "$k: $v" }
        .joinToString(CR_LF, CR_LF)
        .encodeToByteArray()

    private fun ByteArray.indexOf(bytes: ByteArray): Int {
        for (thisIndex in IntRange(0, lastIndex - bytes.lastIndex)) {
            val regionMatches = bytes.foldIndexed(true) { index, acc, byte -> acc && this[thisIndex + index] == byte }
            if (regionMatches) return thisIndex
        }
        return -1
    }

}
