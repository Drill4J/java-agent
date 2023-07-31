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

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import mu.KotlinLogging
import com.epam.drill.common.classloading.ClassSource

class ClassLoadersScanner(
    packagePrefixes: List<String>,
    classesBufferSize: Int,
    transfer: (Set<ClassSource>) -> Unit,
    private val additionalPaths: List<String> = emptyList()
) {

    private val logger = KotlinLogging.logger {}
    private val classPathScanner = ClassPathScanner(packagePrefixes, classesBufferSize, transfer)

    private val getOrLogFail: Result<URI>.() -> URI? = {
        this.onFailure { logger.error(it) { "ClassLoadersScanner: error handling classpath URI" } }
        this.getOrNull()
    }

    fun scanClasses() = scanClassLoadersURIs(scanClassLoaders()).run {
        scanClasses(this + additionalPaths.map(::File).filter(File::exists).map(File::toURI))
    }

    private fun scanClassLoaders() = Thread.getAllStackTraces().keys.mapNotNull(Thread::getContextClassLoader)
        .fold(mutableSetOf(ClassLoader.getSystemClassLoader()), ::addClassLoaderWithParents)

    private fun scanClassLoadersURIs(classloaders: Set<ClassLoader>) = classloaders
        .fold(getSystemClassPath().toMutableSet(), ::addClassLoaderURIs).let(::normalizeURIs)

    private fun scanClasses(uris: Set<URI>) = uris.fold(0, ::addClasses).apply { classPathScanner.transferBuffer() }

    private fun addClassLoaderWithParents(loaders: MutableSet<ClassLoader>, classloader: ClassLoader) = loaders.apply {
        var current: ClassLoader? = classloader
        while (current != null) {
            if (this.add(current)) logger.debug { "ClassLoadersScanner: ClassLoader found: $current" }
            current = current.parent
        }
    }

    private fun addClassLoaderURIs(uris: MutableSet<URI>, classloader: ClassLoader) = uris.apply {
        val toUrlClassloader: (ClassLoader) -> URLClassLoader? = { classloader as? URLClassLoader }
        val toUri: (URL) -> Result<URI> = { it.runCatching { this.toURI() } }
        val logUri: (URI) -> Unit = { logger.debug { "ClassLoadersScanner: ClassLoader URI found: $it" } }
        val addAsUris: (List<URL>) -> Unit = {
            it.map(toUri).mapNotNull(getOrLogFail).filter(this::add).forEach(logUri)
        }
        val result = this.runCatching {
            classloader.let(toUrlClassloader)?.urLs?.toList()?.let(addAsUris)
            classloader.getResources("/").toList().let(addAsUris)
        }
        result.onFailure {
            logger.error(it) { "ClassLoadersScanner: error retrieving classpath URIs from classloader $classloader" }
        }
    }

    private fun addClasses(count: Int, uri: URI) = count + classPathScanner.scanURI(uri)

    private fun getSystemClassPath() = System.getProperty("java.class.path").split(File.pathSeparator).map(::File)
        .filter(File::exists).map(File::toURI)

    private fun normalizeURIs(uris: Set<URI>) = mutableSetOf<URI>().apply {
        val isFileExists: (URI) -> Boolean = { File(it).exists() }
        val isNormalized: (URI) -> Boolean = { uri -> this.any { uri.path.startsWith(it.path) } }
        val toExistingURI: (URI) -> URI? = { it.takeIf(isFileExists) ?: retrieveFileURI(it) }
        uris.map(::normalizeURIPath).mapNotNull(getOrLogFail).forEach {
            it.takeUnless(isNormalized)?.let(toExistingURI)?.let(this::add)
        }
        this.onEach {
            logger.debug { "ClassLoadersScanner: ClassLoader URI normalized: $it" }
        }
    }

    private fun normalizeURIPath(uri: URI) = uri.runCatching {
        val path = this.takeUnless(URI::isOpaque)?.path ?: this.schemeSpecificPart.removePrefix("file:")
        URI("file", null, path.removeSuffix("!/"), null)
    }

    private fun retrieveFileURI(uri: URI) = uri.run {
        val isArchiveContains: (String) -> Boolean = { it.contains(Regex("\\.jar/|\\.war/|\\.rar/|\\.ear/")) }
        val isArchiveEnds: (String) -> Boolean = { it.contains(Regex("\\.jar$|\\.war$|\\.rar$|\\.ear$")) }
        var path = File(this).invariantSeparatorsPath
        while (!File(path).exists() && isArchiveContains(path)) {
            path = path.substringBeforeLast("/")
        }
        path.takeIf(isArchiveEnds)?.let(::File)?.takeIf(File::exists)?.toURI()
    }

}
