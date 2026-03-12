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

class EnvironmentVariablesProviderTest {

    @Test
    fun `to parameter name upper case`() {
        val result = EnvironmentVariablesProvider().toParameterName(SimpleEntry("DRILL_PARAM_NAME", ""))
        assertEquals("paramName", result)
    }

    @Test
    fun `to parameter name lower case`() {
        val result = EnvironmentVariablesProvider().toParameterName(SimpleEntry("DRILL_param_name", ""))
        assertEquals("paramName", result)
    }

    @Test
    fun `to parameter name mixed case`() {
        val result = EnvironmentVariablesProvider().toParameterName(SimpleEntry("DRILL_Param_name_foO_bAr", ""))
        assertEquals("paramNameFooBar", result)
    }

    @Test
    fun `to parameter name underscores`() {
        val result = EnvironmentVariablesProvider().toParameterName(SimpleEntry("DRILL_PARAM_NAME__FOO_BAR", ""))
        assertEquals("paramNameFooBar", result)
    }

    @Test
    fun `parse keys empty`() {
        val result = EnvironmentVariablesProvider().parseKeys(emptyMap())
        assertEquals(0, result.size)
    }

    @Test
    fun `parse keys mixed`() {
        val result = EnvironmentVariablesProvider().parseKeys(mapOf(
            "SOME_VAR" to "value",
            "DRILL_PARAM" to "drillValue",
            "DRILL_PARAM_TWO" to "drillValueTwo"
        ))
        assertEquals(2, result.size)
        assertEquals("drillValue", result["param"])
        assertEquals("drillValueTwo", result["paramTwo"])
    }

    private class SimpleEntry(
        override val key: String,
        override val value: String
    ) : Map.Entry<String, String>

}
