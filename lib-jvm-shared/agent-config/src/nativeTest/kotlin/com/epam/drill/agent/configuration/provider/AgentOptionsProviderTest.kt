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

class AgentOptionsProviderTest {

    @Test
    fun `null options`() {
        val result = AgentOptionsProvider(null).configuration
        assertEquals(0, result.size)
    }

    @Test
    fun `empty options`() {
        val result = AgentOptionsProvider("").configuration
        assertEquals(0, result.size)
    }

    @Test
    fun `simple options`() {
        val result = AgentOptionsProvider("option1=value1,option2=value2").configuration
        assertEquals(2, result.size)
        assertEquals("value1", result["option1"])
        assertEquals("value2", result["option2"])
    }

    @Test
    fun `options with separators`() {
        val result = AgentOptionsProvider("option1=value1,option2=value2;value3;value4,,option3=value3,").configuration
        assertEquals(3, result.size)
        assertEquals("value1", result["option1"])
        assertEquals("value2;value3;value4", result["option2"])
        assertEquals("value3", result["option3"])
    }

    @Test
    fun `options with equals`() {
        val result = AgentOptionsProvider("option1=value1,option2=opt1=val1;opt2=val2,option3=opt3=;opt4=val4,option4=opt5=val5;opt6=").configuration
        assertEquals(4, result.size)
        assertEquals("value1", result["option1"])
        assertEquals("opt1=val1;opt2=val2", result["option2"])
        assertEquals("opt3=;opt4=val4", result["option3"])
        assertEquals("opt5=val5;opt6=", result["option4"])
    }

}