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
package com.epam.drill.agent.test2code.classloading

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassLoadersScannerTest {
    @Test
    fun `given one consumer call, scanClasses should scan classes correctly`() {
        var classCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf("com/example/fixture/classloading/sub"),
            classesBufferSize = 5,
            transfer = { classes ->
                classCount += classes.size
            })
        scanner.scanClasses()
        assertEquals(2, classCount)
    }

    @Test
    fun `given several consumer calls, scanClasses should scan classes correctly`() {
        var classCount = 0
        var transferCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf("com/example/fixture/classloading"),
            classesBufferSize = 5,
            transfer = { classes ->
                classCount += classes.size
                transferCount++
            })
        scanner.scanClasses()
        assertEquals(6, classCount)
        assertEquals(2, transferCount)
    }

    @Test
    fun `given empty package prefix, scanClasses should not scan any classes`() {
        var classCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = emptyList(),
            classesBufferSize = 5,
            transfer = { classes ->
                classCount += classes.size
            })
        scanner.scanClasses()
        assertEquals(0, classCount)
    }

    @Test
    fun `given scan class paths, scanClasses should scan classes`() {
        var classCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf("com/example/fixture/classloading/sub"),
            classesBufferSize = 5,
            scanClassPaths = listOf("build/classes/java/test"),
            enableScanClassLoaders = false,
            transfer = { classes ->
                classCount += classes.size
            })
        scanner.scanClasses()
        assertEquals(2, classCount)
    }

    @Test
    fun `given excluding class paths, scanClasses should not scan classes`() {
        var classCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf("com/example/fixture/classloading"),
            classesBufferSize = 5,
            scanClassPaths = listOf("!build/classes/java/test/"),
            transfer = { classes ->
                classCount += classes.size
            })
        scanner.scanClasses()
        assertEquals(0, classCount)
    }

    @Test
    fun `given excluding class path in sub directory, scanClasses should not scan sub directory`() {
        var classCount = 0
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf("com/example/fixture/classloading"),
            classesBufferSize = 5,
            scanClassPaths = listOf("!build/classes/java/test/com/example/fixture/classloading/sub"),
            transfer = { classes ->
                classCount += classes.size
            })
        scanner.scanClasses()
        assertEquals(4, classCount)
    }

    @Test
    fun `given jar with classes in BOOT-INF classes, scanClasses should scan classes`() {
        val jar = createTempJar("boot-classes.jar") { out ->
            fixtureClasses(SUB_PACKAGE).forEach { (name, bytes) ->
                out.putEntry("BOOT-INF/classes/$name", bytes)
            }
        }
        val scannedClasses = mutableSetOf<String>()
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf(SUB_PACKAGE),
            classesBufferSize = 5,
            scanClassPaths = listOf(jar.absolutePath),
            enableScanClassLoaders = false,
            transfer = { classes ->
                scannedClasses += classes.map { it.entityName() }
            })
        scanner.scanClasses()
        assertEquals(
            setOf("$SUB_PACKAGE/Class1", "$SUB_PACKAGE/Interface1"),
            scannedClasses
        )
    }

    @Test
    fun `given jar with nested jars in BOOT-INF lib, scanClasses should scan classes from nested jars`() {
        val nestedJar1 = jarBytes { out ->
            fixtureClasses(SUB_PACKAGE).forEach { (name, bytes) -> out.putEntry(name, bytes) }
        }
        val nestedJar2 = jarBytes { out ->
            fixtureClasses(ROOT_PACKAGE, recursive = false).forEach { (name, bytes) -> out.putEntry(name, bytes) }
        }
        val jar = createTempJar("boot-lib.jar") { out ->
            out.putEntry("BOOT-INF/lib/nested1.jar", nestedJar1)
            out.putEntry("BOOT-INF/lib/nested2.jar", nestedJar2)
        }
        val scannedClasses = mutableSetOf<String>()
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf(ROOT_PACKAGE),
            classesBufferSize = 5,
            scanClassPaths = listOf(jar.absolutePath),
            enableScanClassLoaders = false,
            transfer = { classes ->
                scannedClasses += classes.map { it.entityName() }
            })
        scanner.scanClasses()
        assertEquals(
            setOf(
                "$SUB_PACKAGE/Class1",
                "$SUB_PACKAGE/Interface1",
                "$ROOT_PACKAGE/Class2",
                "$ROOT_PACKAGE/Class3",
                "$ROOT_PACKAGE/Class4",
                "$ROOT_PACKAGE/Class5",
            ),
            scannedClasses
        )
    }

    @Test
    fun `given executable jar with prepended launch script, scanClasses should scan classes`() {
        val jarContent = jarBytes { out ->
            fixtureClasses(SUB_PACKAGE).forEach { (name, bytes) ->
                out.putEntry("BOOT-INF/classes/$name", bytes)
            }
        }
        val jar = createTempJar("executable.jar") { }.apply {
            writeBytes("#!/bin/bash\necho launch script\nexit 0\n".toByteArray() + jarContent)
        }
        val scannedClasses = mutableSetOf<String>()
        val scanner = ClassLoadersScanner(
            packagePrefixes = listOf(SUB_PACKAGE),
            classesBufferSize = 5,
            scanClassPaths = listOf(jar.absolutePath),
            enableScanClassLoaders = false,
            transfer = { classes ->
                scannedClasses += classes.map { it.entityName() }
            })
        scanner.scanClasses()
        assertEquals(
            setOf("$SUB_PACKAGE/Class1", "$SUB_PACKAGE/Interface1"),
            scannedClasses
        )
    }

    private fun fixtureClasses(packagePath: String, recursive: Boolean = true): Map<String, ByteArray> {
        val root = File(TEST_CLASSES_DIR)
        val dir = File(root, packagePath)
        val files = if (recursive) dir.walkTopDown().filter(File::isFile) else dir.listFiles()!!.asSequence().filter(File::isFile)
        return files.filter { it.extension == "class" }
            .associate { it.toRelativeString(root).replace(File.separatorChar, '/') to it.readBytes() }
            .also { assertTrue(it.isNotEmpty(), "No fixture classes found in ${dir.absolutePath}") }
    }

    private fun createTempJar(name: String, content: (JarOutputStream) -> Unit): File {
        val dir = Files.createTempDirectory("drill-scanner-test").toFile().apply { deleteOnExit() }
        return File(dir, name).apply {
            deleteOnExit()
            writeBytes(jarBytes(content))
        }
    }

    private fun jarBytes(content: (JarOutputStream) -> Unit): ByteArray = ByteArrayOutputStream().also { bytes ->
        JarOutputStream(bytes, Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        }).use(content)
    }.toByteArray()

    private fun JarOutputStream.putEntry(name: String, bytes: ByteArray) {
        putNextEntry(JarEntry(name))
        write(bytes)
        closeEntry()
    }

    private companion object {
        const val TEST_CLASSES_DIR = "build/classes/java/test"
        const val ROOT_PACKAGE = "com/example/fixture/classloading"
        const val SUB_PACKAGE = "com/example/fixture/classloading/sub"
    }

}
