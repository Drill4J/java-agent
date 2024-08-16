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

import com.epam.drill.logging.LoggingConfiguration
import kotlin.test.*

class ValidatedParametersProviderTest {

    @BeforeTest
    fun configureLogger() =
        LoggingConfiguration.setLoggingFilename("build/test-results/validatedParametersProviderTest.log")

    @Test
    fun `validating empty providers`() {
        var e: Throwable? = null
        runCatching { ValidatedParametersProvider(setOf(SimpleMapProvider(emptyMap()))).configuration }
            .onFailure { e = it }
            .onSuccess { e = null }
        assertIs<ParameterValidationException>(e)
    }

    @Test
    fun `validating sum entries`() {
        val default = SimpleMapProvider(defaultParameters())
        val provider1 = SimpleMapProvider(mapOf("foo1" to "bar1"))
        val provider2 = SimpleMapProvider(mapOf("foo2" to "bar2"))
        val result = ValidatedParametersProvider(setOf(default, provider1, provider2)).validatingConfiguration()
        assertEquals(9, result.size)
        assertEquals("bar1", result["foo1"])
        assertEquals("bar2", result["foo2"])
        assertEquals("agent-id", result[DefaultParameterDefinitions.APP_ID.name])
        assertEquals("1.0.0", result[DefaultParameterDefinitions.BUILD_VERSION.name])
        assertEquals("foo/bar", result[DefaultParameterDefinitions.PACKAGE_PREFIXES.name])
        assertEquals("/data/agent", result[DefaultParameterDefinitions.INSTALLATION_DIR.name])
        assertEquals("https://localhost/api", result[ParameterDefinitions.DRILL_API_URL.name])
    }

    @Test
    fun `validating prioritized entries`() {
        val default = SimpleMapProvider(defaultParameters())
        val provider1 = SimpleMapProvider(mapOf("foo1" to "bar11", "foo2" to "bar12"), 100)
        val provider2 = SimpleMapProvider(mapOf("foo2" to "bar22", "foo3" to "bar23"), 200)
        val result = ValidatedParametersProvider(setOf(default, provider1, provider2)).validatingConfiguration()
        assertEquals(10, result.size)
        assertEquals("bar11", result["foo1"])
        assertEquals("bar22", result["foo2"])
        assertEquals("bar23", result["foo3"])
        assertEquals("agent-id", result[DefaultParameterDefinitions.APP_ID.name])
        assertEquals("1.0.0", result[DefaultParameterDefinitions.BUILD_VERSION.name])
        assertEquals("foo/bar", result[DefaultParameterDefinitions.PACKAGE_PREFIXES.name])
        assertEquals("/data/agent", result[DefaultParameterDefinitions.INSTALLATION_DIR.name])
        assertEquals("https://localhost/api", result[ParameterDefinitions.DRILL_API_URL.name])
    }

    @Test
    fun `validate strict parameters correct`() {
        val default = SimpleMapProvider(strictParameters())
        val result = ValidatedParametersProvider(setOf(default)).configuration
        assertEquals(0, result.size)
    }

    @Test
    fun `validate strict parameters missing`() {
        var e: Throwable? = null
        val woAgentId = defaultParameters().also { it.remove(DefaultParameterDefinitions.APP_ID.name) }
        val woPackages = defaultParameters().also { it.remove(DefaultParameterDefinitions.PACKAGE_PREFIXES.name) }
        val woInstallationDir =
            defaultParameters().also { it.remove(DefaultParameterDefinitions.INSTALLATION_DIR.name) }
        val woAdminAddress = defaultParameters().also { it.remove(ParameterDefinitions.DRILL_API_URL.name) }
        runCatching { ValidatedParametersProvider(setOf(SimpleMapProvider(woAgentId))).configuration }
            .onFailure { e = it }
            .onSuccess { e = null }
        assertIs<ParameterValidationException>(e)
        assertIs<ParameterValidationException>(e)
        runCatching { ValidatedParametersProvider(setOf(SimpleMapProvider(woPackages))).configuration }
            .onFailure { e = it }
            .onSuccess { e = null }
        assertIs<ParameterValidationException>(e)
        runCatching { ValidatedParametersProvider(setOf(SimpleMapProvider(woInstallationDir))).configuration }
            .onFailure { e = it }
            .onSuccess { e = null }
        assertIs<ParameterValidationException>(e)
        runCatching { ValidatedParametersProvider(setOf(SimpleMapProvider(woAdminAddress))).configuration }
            .onFailure { e = it }
            .onSuccess { e = null }
        assertIs<ParameterValidationException>(e)
    }

    @Test
    fun `validate soft parameters defaults`() {
        val default = SimpleMapProvider(defaultParameters())
        val soft = SimpleMapProvider(
            mapOf(
                ParameterDefinitions.LOG_LEVEL.name to "foo.bar=UNKNOWN",
                ParameterDefinitions.LOG_LIMIT.name to "-512"
            )
        )
        val result = ValidatedParametersProvider(setOf(default, soft)).configuration
        assertEquals(2, result.size)
        assertEquals(ParameterDefinitions.LOG_LEVEL.defaultValue, result[ParameterDefinitions.LOG_LEVEL.name])
        assertEquals(
            ParameterDefinitions.LOG_LIMIT.defaultValue.toString(),
            result[ParameterDefinitions.LOG_LIMIT.name]
        )
    }

    @Test
    fun `validate soft parameters correct`() {
        assertTrue(ValidatedParametersProvider(setOf(SimpleMapProvider(strictParameters().also {
            it[DefaultParameterDefinitions.BUILD_VERSION.name] = "0.0.1"
        }))).validate().isEmpty())

        assertFalse(ValidatedParametersProvider(setOf(SimpleMapProvider(strictParameters().also {
            it[DefaultParameterDefinitions.BUILD_VERSION.name] = "0. .1"
        }))).validate().isEmpty())

        assertTrue(ValidatedParametersProvider(setOf(SimpleMapProvider(strictParameters().also {
            it[DefaultParameterDefinitions.COMMIT_SHA.name] = "d3f1e2420a0f2f1d9b4c1a4e5a6a1f4d8e3f1b2c"
        }))).validate().isEmpty())

        assertFalse(ValidatedParametersProvider(setOf(SimpleMapProvider(strictParameters().also {
            it[DefaultParameterDefinitions.COMMIT_SHA.name] = "d3f1e24"
        }))).validate().isEmpty())
    }

    @Test
    fun `given correct URL validate drillApiUrl should be valid`() {
        assertTrue(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "https://example.com")))
        assertTrue(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "https://example.com/api")))
        assertTrue(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "http://localhost:8090")))
        assertTrue(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "http://localhost:8090/api")))
    }

    @Test
    fun `given incorrect URL validate drillApiUrl should not be valid`() {
        assertFalse(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "localhost:8090")))
        assertFalse(isValid(mapOf(ParameterDefinitions.DRILL_API_URL.name to "//localhost")))
    }

    private fun isValid(params: Map<String, String>): Boolean {
        return ValidatedParametersProvider(setOf(SimpleMapProvider(strictParameters().also {
            it.putAll(params)
        }))).validate().isEmpty()
    }

    private fun defaultParameters() = mutableMapOf(
        DefaultParameterDefinitions.APP_ID.name to "agent-id",
        DefaultParameterDefinitions.GROUP_ID.name to "group-id",
        DefaultParameterDefinitions.BUILD_VERSION.name to "1.0.0",
        DefaultParameterDefinitions.PACKAGE_PREFIXES.name to "foo/bar",
        DefaultParameterDefinitions.INSTALLATION_DIR.name to "/data/agent",
        ParameterDefinitions.DRILL_API_URL.name to "https://localhost/api",
        ParameterDefinitions.DRILL_API_KEY.name to "apikey",
    )

    private fun strictParameters() = mutableMapOf(
        DefaultParameterDefinitions.GROUP_ID.name to "group-id",
        DefaultParameterDefinitions.APP_ID.name to "agent-id",
        DefaultParameterDefinitions.PACKAGE_PREFIXES.name to "foo/bar",
        ParameterDefinitions.DRILL_API_URL.name to "https://localhost/api",
        DefaultParameterDefinitions.INSTALLATION_DIR.name to "/data/agent",
    )

    private class SimpleMapProvider(
        override val configuration: Map<String, String>,
        override val priority: Int = 100
    ) : AgentConfigurationProvider

}
