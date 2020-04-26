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
