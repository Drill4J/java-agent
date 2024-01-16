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

@ThreadLocal
private val localRequestBytes = mutableMapOf<DRILL_SOCKET, ByteArray?>()

class HttpInterceptor : Interceptor {

    private val httpVerbs = setOf(
        "OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI", HTTP_RESPONSE_MARKER
    )
    private val logger = KotlinLogging.logger("com.epam.drill.agent.interceptor.HttpInterceptor")

    override fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) = try {
        val prefix = bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString()
        val readBytes = { bytes.readBytes(size.convert()) }
        when {
            httpVerbs.any(prefix::startsWith) -> readHttpHeaders(fd, readBytes())
            localRequestBytes[fd] != null -> readHttpHeaders(fd, readBytes())
            else -> Unit
        }
    } catch (e: Exception) {
        logger.error(e) { "interceptRead: $e" }
    }

    override fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) = try {
        val readBytes = bytes.readBytes(size.convert())
        if (readBytes.decodeToString().startsWith("PRI")) {
            TcpFinalData(bytes, size)
        } else {
            val separatorIndex = readBytes.indexOf(CR_LF_BYTES)
            val writeHeaders = injectedHeaders.value()
            if (separatorIndex > 0 && notContainsHeaders(readBytes, writeHeaders)) {
                val responseHead = readBytes.copyOfRange(0, separatorIndex)
                val injectedHeaders = headersToBytes(writeHeaders)
                val responseTail = readBytes.copyOfRange(separatorIndex, size.convert())
                val modified = responseHead + injectedHeaders + responseTail
                logger.trace {
                    val headersRange = modified.indexOf(HEADERS_DELIMITER).takeUnless((-1)::equals) ?: readBytes.size
                    val headersPart = modified.copyOfRange(0, headersRange).decodeToString()
                    "interceptWrite: Writing HTTP headers to fd=$fd: \n${headersPart.prependIndent("\t")}"
                }
                writeCallback.value(modified)
                TcpFinalData(modified.toCValues().getPointer(this), modified.size, injectedHeaders.size)
            } else {
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

    private fun readHttpHeaders(fd: DRILL_SOCKET, readBytes: ByteArray) {
        val bytes = localRequestBytes.getOrElse(fd, ::byteArrayOf)!! + readBytes
        if (notContainsFullHeadersPart(readBytes)) {
            localRequestBytes[fd] = bytes
        } else {
            localRequestBytes.remove(fd)
            val decodedString = bytes.decodeToString()
            logger.trace { "processHttpRequest: Reading HTTP request from fd=$fd:\n${decodedString.prependIndent("\t")}" }
            readHeaders.value(
                decodedString.subSequence(decodedString.indexOf('\r'), decodedString.indexOf(CR_LF + CR_LF))
                    .split(CR_LF)
                    .filter(String::isNotBlank)
                    .map { it.split(":", limit = 2).map(String::trim) }
                    .onEach { logger.trace { "processHttpRequest: Read HTTP header from fd=$fd: ${it[0]}=${it[1]}" } }
                    .associate { it[0].encodeToByteArray() to it[1].encodeToByteArray() }
            )
            readCallback.value(bytes)
        }
    }

    private fun notContainsFullHeadersPart(readBytes: ByteArray) = readBytes.indexOf(HEADERS_DELIMITER) == -1

    private fun notContainsHeaders(readBytes: ByteArray, writeHeaders: Map<String, String>) =
        writeHeaders.isNotEmpty() && readBytes.indexOf(writeHeaders.keys.first().encodeToByteArray()) == -1

    private fun headersToBytes(writeHeaders: Map<String, String>) = writeHeaders.map { (k, v) -> "$k: $v" }
        .joinToString(CR_LF, CR_LF)
        .encodeToByteArray()

    private fun ByteArray.indexOf(array: ByteArray) = run {
        for (thisIndex in IntRange(0, lastIndex - array.size)) {
            val regionMatches = array.foldIndexed(true) { index, acc, byte -> acc && this[thisIndex + index] == byte }
            if (regionMatches) return@run thisIndex
        }
        -1
    }

}
