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

package com.epam.drill.agent.test2code.classparsing

import com.epam.drill.agent.test2code.common.api.AstMethod
import mu.KotlinLogging
import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.Method
import org.jacoco.core.internal.data.CRC64
import java.io.ByteArrayInputStream

val logger = KotlinLogging.logger { }

fun calculateMethodsChecksums(
    classBytes: ByteArray,
    className: String
): Map<String, String> = ClassParser(ByteArrayInputStream(classBytes), className)
    .parse()
    .methods
//    Filter needed for skipping interfaces, which have no opcodes for calculating checksum
    .filter { it.code != null }
    .map { method -> method.classSignature(className) to calculateChecksum(method, className) }
    .filter { it.second != "" }
    .associate { it.first to it.second }

fun Method.classSignature(className: String) = "${className}:${name}:${argumentTypes.asSequence().map { type -> type.toString() }.joinToString(separator = ",")}:${returnType}"

private fun calculateChecksum(
    method: Method,
    className: String
): String {
    try {
        val codeText = method.code.run {
            codeToString(code, constantPool, length, false)
        }
        return CRC64.classId(codeText.toByteArray()).toString(Character.MAX_RADIX)
    } catch (ex: CodeToStringException) {
        logger.error { "Failed to calculate method checksum. Class: $className. Method: ${method.name}. Opcode: ${ex.opcode}. Error: ${ex.error}. Stacktrace: ${ex.stackTraceToString()}" }
        return ""
    }
}

const val CHECKSUM_RADIX = 36
class InvalidChecksumException(checksum: String) : Exception("Invalid checksum value: $checksum")

/**
 * Incrementally combines the CRC64 checksums of all methods of a build into a single build checksum
 * by summing them modulo 2^64 (i.e. relying on natural `Long` overflow), then re-encoding the result the same way.
 */
class CumulativeChecksumCalculator {

    private var sum = 0L
    private var count = 0

    /**
     * The combined build checksum, encoded as a signed base-36 string.
     */
    val methodsChecksum: String
        get() = sum.toString(CHECKSUM_RADIX)

    /**
     * The total number of methods added to the build.
     */
    val methodsCount: Int
        get() = count

    /**
     * Add a single method to the build checksum.
     *
     * @throws InvalidChecksumException if the checksum is not blank and cannot be parsed as base-36.
     */
    fun add(method: AstMethod) {
        count++
        if (method.bodyChecksum.isBlank()) return
        sum += method.bodyChecksum.toLongOrNull(CHECKSUM_RADIX) ?: throw InvalidChecksumException(method.bodyChecksum)
    }
}