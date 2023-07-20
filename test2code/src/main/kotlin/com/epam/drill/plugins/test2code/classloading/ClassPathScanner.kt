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
package com.epam.drill.plugins.test2code.classloading

import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import org.objectweb.asm.ClassReader
import mu.KotlinLogging
import com.epam.drill.common.classloading.ClassSource

private const val PREFIX_SPRING_BOOT = "BOOT-INF/classes/"
private const val PREFIX_WEB_APP = "WEB-INF/classes/"
private const val PACKAGE_DRILL = "com/epam/drill"
private const val PACKAGE_TTL = "com/alibaba/ttl"
private const val JAR_BUFFER_SIZE = 256 * 1024

class ClassPathScanner(
    private val packagePrefixes: List<String>,
    private val classesBufferSize: Int,
    private val transfer: (Set<ClassSource>) -> Unit
) {

    private val logger = KotlinLogging.logger {}
    private val scannedJarFiles = mutableSetOf<String>()
    private val scannedClasses = mutableSetOf<String>()
    private val scannedBuffer = mutableSetOf<ClassSource>()

    private val getOrLogFail: Result<Int>.() -> Int = {
        this.onFailure { logger.error(it) { "ClassPathScanner: error handling class file" } }
        this.getOrDefault(0)
    }

    fun transferBuffer() = scannedBuffer.takeIf(Collection<ClassSource>::isNotEmpty)?.let(transfer)

    fun scanURI(uri: URI) = File(uri).takeIf(File::exists)?.let(::scanFile) ?: 0

    fun scanFile(file: File) = file.takeIf(File::isDirectory)?.let(::scanDirectory) ?: scanJarFile(file).getOrLogFail()

    private fun scanDirectory(file: File) = file.run {
        val isClassFile: (File) -> Boolean = { it.isFile && it.extension == "class" }
        logger.debug { "ClassPathScanner: scanning directory: ${this.absolutePath}" }
        this.walkTopDown().filter(isClassFile).sumOf { scanClassFile(it, this).getOrLogFail() }
    }

    private fun scanJarFile(file: File): Result<Int> = file.runCatching {
        val isNotScanned: (File) -> Boolean = { !scannedJarFiles.contains(it.absolutePath) }
        val fileToStream: (File) -> JarInputStream = { JarInputStream(it.inputStream().buffered(JAR_BUFFER_SIZE)) }
        val pathToFile: (String) -> File? = { File(this.parent, it).takeIf(File::exists) }
        var scanned = 0
        logger.debug { "ClassPathScanner: scanning file: ${this.absolutePath}" }
        this.takeIf(isNotScanned)?.let(fileToStream)?.use {
            scanned += scanJarInputStream(it).also { scannedJarFiles.add(this.absolutePath) }
            scanned += it.manifest?.mainAttributes?.getValue(Attributes.Name.CLASS_PATH)?.split(" ")
                ?.mapNotNull(pathToFile)?.map(::scanJarFile)?.sumOf(getOrLogFail) ?: 0
        }
        scanned
    }

    private fun scanJarInputStream(stream: JarInputStream) = stream.run {
        var scanned = 0
        var jarEntry = this.nextJarEntry
        while (jarEntry != null) {
            when (jarEntry.takeUnless(JarEntry::isDirectory)?.name?.substringAfterLast('.')) {
                "jar", "war", "rar" -> scanned += scanJarEntry(jarEntry, this.readBytes()).getOrLogFail()
                "class" -> scanned += scanClassEntry(jarEntry, this.readBytes()).getOrLogFail()
            }
            jarEntry = this.nextJarEntry
        }
        scanned
    }

    private val isPrefixMatches: (ClassSource) -> Boolean = { it.prefixMatches(packagePrefixes) }
    private val isClassAccepted: (ClassSource) -> Boolean = {
        !it.entityName().contains('$') &&
                !it.entityName().startsWith(PACKAGE_DRILL) &&
                !it.entityName().startsWith(PACKAGE_TTL) &&
                it.prefixMatches(packagePrefixes) &&
                !scannedClasses.contains(it.entityName())
    }

    private fun scanClassFile(file: File, directory: File) = file.runCatching {
        val readClassSource: (ClassSource) -> ClassSource? = {
            val bytes = this.readBytes()
            val superName = ClassReader(bytes).superName ?: ""
            it.copy(superName = superName, bytes = bytes)
        }
        logger.trace { "ClassPathScanner: scanning class file: ${this.toRelativeString(directory)}" }
        this.toRelativeString(directory).replace(File.separatorChar, '/')
            .removePrefix(PREFIX_WEB_APP).removePrefix(PREFIX_SPRING_BOOT).removeSuffix(".class").let(::ClassSource)
            .takeIf(isClassAccepted)?.let(readClassSource)?.takeIf(isPrefixMatches)?.let(::addClassToScanned) ?: 0
    }

    private fun scanClassEntry(entry: JarEntry, bytes: ByteArray) = entry.name.runCatching {
        val readClassSource: (ClassSource) -> ClassSource? = {
            val superName = ClassReader(bytes).superName ?: ""
            it.copy(superName = superName, bytes = bytes)
        }
        logger.trace { "ClassPathScanner: scanning class entry: $this" }
        this.removePrefix(PREFIX_WEB_APP).removePrefix(PREFIX_SPRING_BOOT).removeSuffix(".class").let(::ClassSource)
            .takeIf(isClassAccepted)?.let(readClassSource)?.takeIf(isPrefixMatches)?.let(::addClassToScanned) ?: 0
    }

    private fun scanJarEntry(entry: JarEntry, bytes: ByteArray): Result<Int> = entry.name.runCatching {
        logger.debug { "ClassPathScanner: scanning jar entry: $this" }
        JarInputStream(ByteArrayInputStream(bytes)).use(::scanJarInputStream)
    }

    private fun addClassToScanned(classSource: ClassSource): Int {
        val isBufferFilled: (Set<ClassSource>) -> Boolean = { it.size >= classesBufferSize }
        logger.trace { "ClassPathScanner: found class: ${classSource.entityName()}" }
        scannedClasses.add(classSource.entityName())
        scannedBuffer.add(classSource)
        scannedBuffer.takeIf(isBufferFilled)?.also(transfer)?.clear()
        return 1
    }

}
