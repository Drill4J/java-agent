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

import kotlinx.cinterop.memScoped
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

class PropertiesFileProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = 200
) : AgentConfigurationProvider {

    @OptIn(ExperimentalNativeApi::class)
    private val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"
    private val defaultPath = ".${pathSeparator}drill.properties"

    override val configuration = configuration()

    private fun configuration() = configPath().runCatching(::readFile).getOrNull()
        ?.let(::parseLines)
        ?: emptyMap()

    @OptIn(ExperimentalForeignApi::class)
    private fun readFile(filepath: String) = memScoped {
        val file = open(filepath, O_RDONLY)
        Input(file).readText().also { close(file) }
    }

    internal fun parseLines(text: String): Map<String, String> {
        return text
            // normalize line endings
            .replace("\r\n", "\n").replace("\r", "\n")
            // remove comment lines
            .replace(Regex("""(?<=\n)\s*#.*\n"""), "")
            // compact multiline values into corresponding single lines
            // regex matches backslash at the end of line surrounded by any number of whitespaces
            .replace(Regex("""\s*\\\s*\n"""), "")
            .lines()
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
    }

    internal fun configPath() = fromProviders()
        ?: fromInstallationDir()
        ?: defaultPath

    internal fun fromProviders() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultParameterDefinitions.CONFIG_PATH.name] }
        .lastOrNull()

    internal fun fromInstallationDir() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultParameterDefinitions.INSTALLATION_DIR.name] }
        .lastOrNull()
        ?.let { "${it}${pathSeparator}drill.properties" }

}
