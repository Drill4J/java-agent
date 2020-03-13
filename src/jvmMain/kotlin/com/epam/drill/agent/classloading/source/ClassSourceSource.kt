package com.epam.drill.agent.classloading.source

import java.io.*
import java.net.*
import java.util.*
import java.util.jar.*

class ClassLoaderSource(override val className: String, private val classLoader: ClassLoader) : ClassSource() {

    override fun bytes(): ByteArray {
        return classLoader.url("$className.class").readBytes()
    }

}

class JarSource(override val className: String, private val file: File) : ClassSource() {

    override fun bytes(): ByteArray {
        return JarFile(file).use { it.getInputStream(it.getJarEntry("$className.class")).readBytes() }
    }

}

class FileSource(override val className: String, private val file: File) : ClassSource() {

    override fun bytes(): ByteArray {
        return file.readBytes()
    }

}

abstract class ClassSource : ByteSource {
    abstract val className: String
    override fun toString() = "$className: ${this::class.simpleName}"
}

interface ByteSource {

    fun bytes(): ByteArray

}


private fun ClassLoader.url(resourceName: String): URL {
    return getResource(resourceName) ?: throw NoSuchElementException(resourceName)
}
