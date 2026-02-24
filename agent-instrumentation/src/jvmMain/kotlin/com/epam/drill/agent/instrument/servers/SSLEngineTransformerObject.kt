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
package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import java.nio.ByteBuffer
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.common.request.HeadersRetriever
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_SSL_ENABLED
import com.epam.drill.agent.instrument.SSL_ENGINE_CLASS_NAME

private const val HTTP_DETECTOR_BYTES_COUNT = 8
private const val HTTP_HEADERS_SEPARATOR = "\r\n"
private const val HTTP_HEADERS_END_MARK = HTTP_HEADERS_SEPARATOR + HTTP_HEADERS_SEPARATOR
private const val HTTP_RESPONSE_MARK = "HTTP"

/**
 * Transformer for SSLEngine based web servers with java-side HTTPS termination
 *
 * Tested with:
 *     jdk 1.8.0_241
 */
abstract class SSLEngineTransformerObject(
    headersRetriever: HeadersRetriever,
    agentConfiguration: AgentConfiguration
) : HeadersProcessor, AbstractTransformerObject(agentConfiguration) {

    private val httpVerbs = setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI")
    private val httpHeadersEndMark = HTTP_HEADERS_END_MARK.encodeToByteArray()

    private val agentIdPair by lazy { headersRetriever.agentIdHeader() to headersRetriever.agentIdHeaderValue() }
    private val adminAddressPair by lazy { headersRetriever.adminAddressHeader() to headersRetriever.adminAddressValue() }

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_SSL_ENABLED]

    override fun permit(
        className: String,
        superName: String?,
        interfaces: Array<String?>
    ): Boolean = superName == SSL_ENGINE_CLASS_NAME

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod(
            "unwrap",
            "(Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;II)Ljavax/net/ssl/SSLEngineResult;"
        ).insertCatching(
            CtBehavior::insertAfter,
            """
            ${this::class.java.name}.INSTANCE.${this::readHttpRequest.name}($2);
            """.trimIndent()
        )
        val wrapMethod = ctClass.getMethod(
            "wrap",
            "([Ljava/nio/ByteBuffer;IILjava/nio/ByteBuffer;)Ljavax/net/ssl/SSLEngineResult;"
        )
        wrapMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            ${this::class.java.name}.INSTANCE.${this::writeHttpRequest.name}($1);
            """.trimIndent()
        )
        wrapMethod.insertCatching(
            CtBehavior::insertAfter,
            """
            ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            """.trimIndent()
        )
    }

    fun readHttpRequest(buffers: Array<ByteBuffer>) = try {
        val prefix = retrievePrefix(buffers[0])
        val bytes by lazy { retrieveBytes(buffers[0]) }
        val headersEnd by lazy { bytes.indexOf(httpHeadersEndMark) }
        when {
            httpVerbs.any(prefix::startsWith) && headersEnd != -1 -> readHeaders(bytes, headersEnd)
            else -> Unit
        }
    } catch (e: Exception) {
        logger.error(e) { "readHttpRequest: Error while parse request buffer" }
    }

    fun writeHttpRequest(buffers: Array<ByteBuffer>) = try {
        val prefix = retrievePrefix(buffers[0])
        val bytes by lazy { retrieveBytes(buffers[0]) }
        val headersEnd by lazy { bytes.indexOf(httpHeadersEndMark) }
        when {
            prefix.startsWith(HTTP_RESPONSE_MARK) && headersEnd != -1 -> writeHeaders(buffers, bytes, headersEnd)
            else -> Unit
        }
    } catch (e: Exception) {
        logger.error(e) { "writeHttpRequest: Error while writing response buffer" }
    }

    private fun retrievePrefix(buffer: ByteBuffer) = Integer.min(HTTP_DETECTOR_BYTES_COUNT, buffer.limit())
        .let { buffer.array().copyOfRange(buffer.arrayOffset(), it).decodeToString() }

    private fun retrieveBytes(buffer: ByteBuffer) = buffer.array()
        .copyOfRange(buffer.arrayOffset(), buffer.arrayOffset() + buffer.limit())

    private fun readHeaders(bytes: ByteArray, index: Int) = bytes.copyOf(index).decodeToString()
        .also { logger.trace { "readHeaders: Reading HTTP request:\n${it.prependIndent("\t")}" } }
        .substringAfter(HTTP_HEADERS_SEPARATOR) // skip first line with HTTP response status
        .split(HTTP_HEADERS_SEPARATOR)
        .filter(String::isNotBlank)
        .map { it.split(":", limit = 2).map(String::trim) }
        .onEach { logger.trace { "readHeaders: Read HTTP header: ${it[0]}=${it[1]}" } }
        .associate { it[0] to it[1] }
        .let(::storeHeaders)

    private fun writeHeaders(buffers: Array<ByteBuffer>, bytes: ByteArray, index: Int) {
        val headers = (retrieveHeaders() ?: emptyMap()) + mapOf(agentIdPair, adminAddressPair)
        if (bytes.indexOf(headers.keys.first().encodeToByteArray()) != -1) return
        headers.entries.forEach { logger.trace { "writeHeaders: Writing HTTP header: ${it.key}=${it.value}" } }
        val responseHead = bytes.copyOfRange(0, index)
        val responseTail = bytes.copyOfRange(index, bytes.size)
        val injectedHeaders = headers.map { (k, v) -> "$k: $v" }
            .joinToString(HTTP_HEADERS_SEPARATOR, HTTP_HEADERS_SEPARATOR)
            .encodeToByteArray()
        val modified = responseHead + injectedHeaders + responseTail
        buffers[0].position(bytes.size)
        buffers[0] = ByteBuffer.wrap(modified)
        logger.trace { "writeHeaders: Written HTTP headers:\n${modified.decodeToString().prependIndent("\t")}" }
    }

    private fun ByteArray.indexOf(bytes: ByteArray): Int {
        for (thisIndex in IntRange(0, lastIndex - bytes.lastIndex)) {
            val regionMatches = bytes.foldIndexed(true) { index, acc, byte -> acc && this[thisIndex + index] == byte }
            if (regionMatches) return thisIndex
        }
        return -1
    }

}
