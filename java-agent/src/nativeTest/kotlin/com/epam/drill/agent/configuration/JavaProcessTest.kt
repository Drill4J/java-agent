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