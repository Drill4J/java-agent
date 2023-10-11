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
package com.epam.drill.agent.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentDirParsingTest {

    @Test
    fun `platform specific style path`() =
        if (Platform.osFamily == OsFamily.WINDOWS) {
            val agentDir = parseAgentDirFromAgentPathCommand(
                "-agentpath:C:\\data\\agent\\drill_agent.dll=param1=1,param2=2",
                pathSeparator
            )
            assertEquals("C:\\data\\agent", agentDir)
        } else {
            val agentDir = parseAgentDirFromAgentPathCommand(
                "-agentpath:/data/agent/libdrill_agent.so=param1=1,param2=2",
                pathSeparator
            )
            assertEquals("/data/agent", agentDir)
        }

    @Test
    fun `without options`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:/data/agent/libdrill_agent.so")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with equal separator without options`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:/data/agent/libdrill_agent.so=")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with space separator without options`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:/data/agent/libdrill_agent.so -Dparam=foo/bar")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with options`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:/data/agent/libdrill_agent.so=opt1,opt2")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `without leading directory separator`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:libdrill_agent.so")
        assertNull(agentDir)
    }

    @Test
    fun `with other arguments`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-Dparam=foo/bar -agentpath:/data/agent/libdrill_agent.so /other/path")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with quotes`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:\"/data/agent/libdrill_agent.so=opt1,opt2\"")
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with quotes and spaces`() {
        val agentDir = parseAgentDirFromAgentPathCommand("-agentpath: \"/data space/agent/libdrill_agent.so\" /other/path")
        assertEquals("/data space/agent", agentDir)
    }

    @Test
    fun `with multi lines`() {
        val agentDir = parseAgentDirFromAgentPathCommand(
            """java -jar some.jar
                        
            -agentpath:/data/agent/libdrill_agent.so
        """
        )
        assertEquals("/data/agent", agentDir)
    }

    @Test
    fun `with multi agents`() {
        val agentDir = parseAgentDirFromAgentPathCommand(
            """            
            -agentpath:/other/path/other_agent.dll
            -agentpath:/data/agent/libdrill_agent.so
            -agentpath:/another/path/libanother_agent.so
        """.trimIndent()
        )
        assertEquals("/data/agent", agentDir)
    }
}
