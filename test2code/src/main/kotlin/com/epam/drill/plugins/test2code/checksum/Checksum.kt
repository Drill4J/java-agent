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
package com.epam.drill.plugins.test2code.checksum

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.Method
import org.jacoco.core.internal.data.CRC64
import java.io.ByteArrayInputStream

internal fun calculateMethodsChecksums(
    classBytes: ByteArray,
    className: String
): Map<String, String> = ClassParser(ByteArrayInputStream(classBytes), className)
    .parse()
    .methods
//    Filter needed for skipping interfaces, which have no opcodes for calculating checksum
    .filter { it.code != null }
    .associate { method -> method.classSignature() to calculateChecksum(method) }

private fun Method.classSignature() =
    "${name}/${argumentTypes.asSequence().map { type -> type.toString() }.joinToString()}/${returnType}"

private fun calculateChecksum(
    method: Method,
): String {
    val codeText = method.code.run {
        codeToString(code, constantPool, 0, length, false)
    }
    return CRC64.classId(codeText.toByteArray()).toString(Character.MAX_RADIX)
}