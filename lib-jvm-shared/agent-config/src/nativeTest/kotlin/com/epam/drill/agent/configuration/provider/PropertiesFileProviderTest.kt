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
package com.epam.drill.agent.configuration.provider

import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.*

class PropertiesFileProviderTest {

    @OptIn(ExperimentalNativeApi::class)
    private val separator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"
    @OptIn(ExperimentalNativeApi::class)
    private val fullPath = if (Platform.osFamily == OsFamily.WINDOWS) "C:\\data\\agent" else "/data/agent"
    @OptIn(ExperimentalNativeApi::class)
    private val testPath1 = if (Platform.osFamily == OsFamily.WINDOWS) "C:\\data1" else "/data1"
    @OptIn(ExperimentalNativeApi::class)
    private val testPath2 = if (Platform.osFamily == OsFamily.WINDOWS) "C:\\data2" else "/data2"

    @Test
    fun `parse empty lines`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse mixed lines`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=bar1            
            #foo2=bar2            

            foo3=bar3            
            #foo4=bar4            
        """)
        assertEquals(2, result.size)
        assertEquals("bar1", result["foo1"])
        assertEquals("bar3", result["foo3"])
    }

    @Test
    fun `parse multiline properties`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=fiz\
            buz
        """.trimIndent())
        assertEquals(1, result.size)
        assertEquals("fizbuz", result["foo1"])
    }

    @Test
    fun `parse multiline properties - with spaces around backslash`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=fiz \ 
            buz    \
            fuz
        """.trimIndent())
        assertEquals(1, result.size)
        assertEquals("fizbuzfuz", result["foo1"])
    }

    @Test
    fun `parse multiline properties - with other properties around`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo0=bar0
            
            foo1=fiz\ 
            buz\
            fuz
            
            foo2=bar2
        """.trimIndent())
        assertEquals(3, result.size)
        assertEquals("bar0", result["foo0"])
        assertEquals("fizbuzfuz", result["foo1"])
        assertEquals("bar2", result["foo2"])
    }

    // Ignored - we indent around multiline values does break parsers for now
    @Test @Ignore
    fun `parse multiline properties - with indent around values`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo0=bar0

            foo1=fiz\ 
            buz\
            fuz

            foo2=bar2
        """)
        assertEquals(3, result.size)
        assertEquals("bar0", result["foo0"])
        assertEquals("fizbuzfuz", result["foo1"])
        assertEquals("bar2", result["foo2"])
    }

    @Test
    fun `parse lines with comments containing backslashes`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=bar1
            foo2=bar2
            # \ must not break parsing
            foo3=bar3            
        """)
        assertEquals("bar1", result["foo1"])
        assertEquals("bar2", result["foo2"])
        assertEquals("bar3", result["foo3"])
        assertEquals(3, result.size)
    }

    @Test
    fun `hash symbol at the beginning of the line is a comment`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            # this is a comment
            foo1=bar1
        """)
        assertEquals("bar1", result["foo1"])
        assertEquals(1, result.size)
    }

    @Test
    fun `hash symbol after = must be treated as part of a value`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1=bar1#baz
        """)
        assertEquals("bar1#baz", result["foo1"])
        assertEquals(1, result.size)
    }

//     TODO for now we allow hash symbol in a property name, it does not break the parsing
//      there might be some edge cases to that
    @Test @Ignore
    fun `unescaped hash symbol is illegal in a property name`() {
        assertFailsWith<IllegalArgumentException> {
            PropertiesFileProvider(emptySet()).parseLines("""
            foo1#bar=baz
        """)
        }
    }

    @Test @Ignore
    fun `escaped hash symbol is allowed in a property name`() {
        val result = PropertiesFileProvider(emptySet()).parseLines("""
            foo1\#bar=baz
        """)
        assertEquals("baz", result["foo1bar"])
        assertEquals(1, result.size)
    }

    @Test
    fun `config path default`() {
        val result = PropertiesFileProvider(emptySet()).configPath()
        assertEquals(".${separator}drill.properties", result)
    }

    @Test
    fun `config path from installation dir`() {
        val result = PropertiesFileProvider(setOf(SimpleMapProvider(mapOf(
            DefaultParameterDefinitions.INSTALLATION_DIR.name to fullPath
        )))).configPath()
        assertEquals("$fullPath${separator}drill.properties", result)
    }

    @Test
    fun `config path from property`() {
        val result = PropertiesFileProvider(setOf(SimpleMapProvider(mapOf(
            DefaultParameterDefinitions.INSTALLATION_DIR.name to fullPath,
            DefaultParameterDefinitions.CONFIG_PATH.name to "$fullPath${separator}config.properties"
        )))).configPath()
        assertEquals("$fullPath${separator}config.properties", result)
    }

    @Test
    fun `from providers empty providers`() {
        val result = PropertiesFileProvider(emptySet()).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers no entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo" to "bar2"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers one entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "/config1.properties"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("/config1.properties", result)
    }

    @Test
    fun `from providers two entry prioritized`() {
        val provider1 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "/config1.properties"), 100)
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.CONFIG_PATH.name to "/config2.properties"), 200)
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("/config2.properties", result)
    }

    @Test
    fun `from installation dir empty providers`() {
        val result = PropertiesFileProvider(emptySet()).fromInstallationDir()
        assertNull(result)
    }

    @Test
    fun `from installation dir no entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo" to "bar2"))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertNull(result)
    }

    @Test
    fun `from installation dir one entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to testPath1))
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertEquals("$testPath1${separator}drill.properties", result)
    }

    @Test
    fun `from installation dir two entry prioritized`() {
        val provider1 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to testPath1), 100)
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to testPath2), 200)
        val result = PropertiesFileProvider(setOf(provider1, provider2)).fromInstallationDir()
        assertEquals("$testPath2${separator}drill.properties", result)
    }

    private class SimpleMapProvider(
        override val configuration: Map<String, String>,
        override val priority: Int = 100
    ) : AgentConfigurationProvider

}
