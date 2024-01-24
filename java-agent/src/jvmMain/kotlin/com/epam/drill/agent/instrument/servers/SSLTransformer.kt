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

import java.nio.ByteBuffer
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.request.DrillRequest

actual object SSLTransformer : TransformerObject, AbstractTransformerObject() {

    private const val HTTP_DETECTOR_BYTES_COUNT = 8
    private val HTTP_VERBS = setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI")
    private val HTTP_HEADERS_SEPARATOR = "\r\n"
    private val HTTP_HEADERS_END_MARK = "\r\n\r\n".encodeToByteArray()

    override val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        throw NotImplementedError()

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("unwrap","(Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;II)Ljavax/net/ssl/SSLEngineResult;")
            .insertCatching(
                CtMethod::insertAfter,
                """
                   ${this::class.java.name}.INSTANCE.${this::parseHttpRequest.name}($2);
                """.trimIndent()
            )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun parseHttpRequest(buffers: Array<ByteBuffer>) = try {
        val bytes = buffers[0].array()
        val prefix = bytes.copyOf(HTTP_DETECTOR_BYTES_COUNT).decodeToString()
        when {
            HTTP_VERBS.any(prefix::startsWith) -> {
                val headers = bytes.indexOf(HTTP_HEADERS_END_MARK).takeUnless((-1)::equals)
                    ?.let(bytes::copyOf)?.decodeToString()
                    ?.split(HTTP_HEADERS_SEPARATOR)?.drop(1)?.filter(String::isNotBlank)
                    ?.map { it.split(":", limit = 2).map(String::trim) }
                    ?.associate { it[0] to it[1] }
                headers?.get(HeadersRetriever.sessionHeader())?.also {
                    RequestHolder.store(DrillRequest(it, headers))
                }
            }
            else -> Unit
        }
    } catch(e: Throwable) {
        logger.error(e) { "parseHttpRequest: Error while parse request buffer" }
    }

    private fun ByteArray.indexOf(bytes: ByteArray): Int {
        for (thisIndex in IntRange(0, lastIndex - bytes.lastIndex)) {
            val regionMatches = bytes.foldIndexed(true) { index, acc, byte -> acc && this[thisIndex + index] == byte }
            if (regionMatches) return thisIndex
        }
        return -1
    }

}
