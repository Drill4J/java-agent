package com.epam.drill.agent.configuration

import com.epam.drill.agent.configuration.process.JavaProcess
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaProcessTest {
    @Test
    fun `check getting drill agent path on windows`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "some/native/agent",
                "/some/folder/drill_agent.dll,key1=value1,key2=value2"
            )
        )

        assertEquals("/some/folder", command.getDrillAgentPath("drill-agent"))
    }

    @Test
    fun `check getting drill agent path on linux`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "some/native/agent",
                "/some/folder/libdrill_agent.so,key1=value1,key2=value2"
            )
        )

        assertEquals("/some/folder", command.getDrillAgentPath("drill-agent"))
    }
}