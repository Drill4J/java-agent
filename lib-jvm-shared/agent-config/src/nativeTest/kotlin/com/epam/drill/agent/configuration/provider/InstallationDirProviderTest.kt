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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import kotlin.experimental.ExperimentalNativeApi

class InstallationDirProviderTest {

    @OptIn(ExperimentalNativeApi::class)
    private val separator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"
    @OptIn(ExperimentalNativeApi::class)
    private val libName = if (Platform.osFamily == OsFamily.WINDOWS) "drill_agent.dll" else "libdrill_agent.so"
    @OptIn(ExperimentalNativeApi::class)
    private val fullPath = if (Platform.osFamily == OsFamily.WINDOWS) "C:\\data\\agent" else "/data/agent"
    @OptIn(ExperimentalNativeApi::class)
    private val rootPath = if (Platform.osFamily == OsFamily.WINDOWS) "C:" else "/"
    @OptIn(ExperimentalNativeApi::class)
    private val rootLibPath = if (Platform.osFamily == OsFamily.WINDOWS) "C:\\drill_agent.dll" else "/libdrill_agent.so"

    @Test
    fun `parse platform specific style path`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:$fullPath$separator$libName=param1=1,param2=2")
        assertEquals(fullPath, result)
    }

    @Test
    fun `parse with root directory`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:$rootLibPath")
        assertEquals(rootPath, result)
    }

    @Test
    fun `parse without options`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:/data/agent/$libName")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with equal separator without options`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:/data/agent/$libName=")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with space separator without options`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:/data/agent/$libName -Dparam=foo/bar")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with options`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:/data/agent/$libName=opt1,opt2")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse without leading directory separator`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:$libName")
        assertEquals(".", result)
    }

    @Test
    fun `parse with other arguments`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-Dparam=foo/bar -agentpath:/data/agent/$libName /other/path")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with quotes`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath:\"/data/agent/$libName=opt1,opt2\"")
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with quotes and spaces`() {
        val result = InstallationDirProvider(emptySet())
            .parse("-agentpath: \"/data space/agent/$libName\" /other/path")
        assertEquals("/data space/agent", result)
    }

    @Test
    fun `parse with multi lines`() {
        val result = InstallationDirProvider(emptySet()).parse(
            """
            java -jar some.jar
            
            -agentpath:/data/agent/$libName
            """
        )
        assertEquals("/data/agent", result)
    }

    @Test
    fun `parse with multi agents`() {
        val result = InstallationDirProvider(emptySet()).parse(
            """            
            -agentpath:/other/path/other_agent.dll
            -agentpath:/data/agent/$libName
            -agentpath:/another/path/libanother_agent.so
            """.trimIndent()
        )
        assertEquals("/data/agent", result)
    }

    @Test
    fun `from providers empty providers`() {
        val result = InstallationDirProvider(emptySet()).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers no entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo" to "bar2"))
        val result = InstallationDirProvider(setOf(provider1, provider2)).fromProviders()
        assertNull(result)
    }

    @Test
    fun `from providers one entry`() {
        val provider1 = SimpleMapProvider(mapOf("foo" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "/foo/bar"))
        val result = InstallationDirProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("/foo/bar", result)
    }

    @Test
    fun `from providers two entry prioritized`() {
        val provider1 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "/foo/bar1"), 100)
        val provider2 = SimpleMapProvider(mapOf(DefaultParameterDefinitions.INSTALLATION_DIR.name to "/foo/bar2"), 200)
        val result = InstallationDirProvider(setOf(provider1, provider2)).fromProviders()
        assertEquals("/foo/bar2", result)
    }

    private class SimpleMapProvider(
        override val configuration: Map<String, String>,
        override val priority: Int = 100
    ) : AgentConfigurationProvider

}
