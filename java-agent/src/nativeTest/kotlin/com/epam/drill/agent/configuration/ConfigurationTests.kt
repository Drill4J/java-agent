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
