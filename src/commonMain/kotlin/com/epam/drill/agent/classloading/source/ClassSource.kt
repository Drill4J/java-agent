/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent.classloading.source

private const val SUBCLASS = "!subclassOf:"

interface ByteSource {
    fun bytes(): ByteArray
}

data class ClassSource(
    val className: String,
    private val bytes: ByteArray = byteArrayOf(),
    val superName: String = ""
) : ByteSource {
    override fun bytes() = bytes

    override fun toString() = "$className: ${this::class.simpleName}"

    override fun equals(other: Any?) = other is ClassSource && className == other.className

    override fun hashCode() = className.hashCode()
}

fun String.toClassSource() = ClassSource(this)

fun ClassSource.matches(
    others: Iterable<String>, thisOffset: Int = 0
): Boolean = others.any {
    className.regionMatches(thisOffset, it, 0, it.length)
} && others.none {
    it.startsWith(SUBCLASS) && superName.regionMatches(thisOffset, it, SUBCLASS.length, it.length - SUBCLASS.length)
} && others.none {
    it.startsWith('!') && className.regionMatches(thisOffset, it, 1, it.length - 1)
}
