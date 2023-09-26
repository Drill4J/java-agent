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

import com.epam.drill.agent.configuration.process.JavaProcess
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaProcessTest {
    @Test
    fun `given in any order native agent getDrillAgentPath must find the agent path by drillLibName`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "/first/native/agent1.so",
                "/second/native/agent2.so",
                "/third/native/agent3.so",
            )
        )

        assertEquals("/second/native", command.getDrillAgentPath("agent2"))
    }

    @Test
    fun `given a native agent with an underscore in the name getDrillAgentPath must find the agent path by drillLibName with dashes`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "/some/native/libdrill_agent.so"
            )
        )

        assertEquals("/some/native", command.getDrillAgentPath("drill-agent"))
    }

    @Test
    fun `given a native agent with arguments getDrillAgentPath must find the agent path`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "/some/native/agent.so=key1=value1,key2=value2"
            )
        )

        assertEquals("/some/native", command.getDrillAgentPath("agent"))
    }

    @Test
    fun `given a native agent in quotes getDrillAgentPath must find the agent path`() {
        val command = JavaProcess(
            nativeAgents = mutableListOf(
                "\"/some/native/agent.so\""
            )
        )

        assertEquals("/some/native", command.getDrillAgentPath("agent"))
    }
}