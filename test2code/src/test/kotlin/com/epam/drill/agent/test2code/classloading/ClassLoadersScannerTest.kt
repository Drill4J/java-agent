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

import kotlin.test.Test
import kotlin.test.assertEquals

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

}
