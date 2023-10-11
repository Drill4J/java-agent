package com.epam.drill.agent.configuration

import kotlin.test.Test
import kotlin.test.assertEquals

class AgentDirParsingTest {

    @Test
    fun `platform specific style path`() =
        if (Platform.osFamily == OsFamily.WINDOWS) {
            val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:C:\\data\\agent\\drill_agent.dll=param1=1,param2=2")
            assertEquals("C:\\data\\agent", agentDir)
        } else {
            val agentDir = parseAgentDirFromAgentPathCommand("-agentpath:/data/agent/libdrill_agent.so=param1=1,param2=2")
            assertEquals("/data/agent", agentDir)
        }

    @Test
    fun `without options`() {
        given("-agentpath:/data/agent/libdrill_agent.so")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `with equal separator without options`() {
        given("-agentpath:/data/agent/libdrill_agent.so=")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `with space separator without options`() {
        given("-agentpath:/data/agent/libdrill_agent.so -Dparam=foo/bar")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `with options`() {
        given("-agentpath:/data/agent/libdrill_agent.so=opt1,opt2")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `without leading directory separator`() {
        given("-agentpath:libdrill_agent.so")
            .agentDirShouldBe(null)
    }

    @Test
    fun `with other arguments`() {
        given("-Dparam=foo/bar -agentpath:/data/agent/libdrill_agent.so /other/path")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `with quotes`() {
        given("-agentpath:\"/data/agent/libdrill_agent.so=opt1,opt2\"")
            .agentDirShouldBe("/data/agent")
    }

    @Test
    fun `with quotes and spaces`() {
        given("-agentpath: \"/data space/agent/libdrill_agent.so\" /other/path")
            .agentDirShouldBe("/data space/agent")
    }

    @Test
    fun `with multi lines`() {
        given("""java -jar some.jar
                        
            -agentpath:/data/agent/libdrill_agent.so
        """)
            .agentDirShouldBe("/data/agent")
    }

    private fun given(agentPathCommand: String): String? {
        return parseAgentDirFromAgentPathCommand(agentPathCommand.adaptToPlatformPathSeparator())
    }

    private fun String?.agentDirShouldBe(expected: String?) {
        assertEquals(expected?.adaptToPlatformPathSeparator(), this)
    }

    private fun String.adaptToPlatformPathSeparator() = this.replace("/", pathSeparator)
}
