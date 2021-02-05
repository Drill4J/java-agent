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

import java.io.*

interface ByteSource {
    fun bytes(): ByteArray
}

abstract class ClassSource : ByteSource {

    abstract val className: String

    override fun toString() = "$className: ${this::class.simpleName}"

    override fun equals(other: Any?) = other is ClassSource && className == other.className

    override fun hashCode() = className.hashCode()
}

class FileSource(override val className: String, private val file: File) : ClassSource() {
    override fun bytes(): ByteArray {
        return file.readBytes()
    }

}

class ByteArrayClassSource(
    override val className: String,
    private val bytes: ByteArray
) : ClassSource() {
    override fun bytes() = bytes
}
