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

import kotlinx.cinterop.toKString
import platform.posix.getenv
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.AgentProcessMetadata
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import kotlinx.cinterop.ExperimentalForeignApi

class InstallationDirProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = 300,
    agentLibName: String = "drill-agent"
) : AgentConfigurationProvider {

    private val agentLibNamePattern = "(lib)?${agentLibName.replace("-", "_")}(\\.so|\\.dll|\\.dylib)"
    private val agentLibPathRegex = Regex("[\"]?(.*[/\\\\])?$agentLibNamePattern([ =\"].*)?")

    override val configuration = mapOf(Pair(DefaultParameterDefinitions.INSTALLATION_DIR.name, installationDir()))

    private fun installationDir() = fromProviders()
        ?: fromJavaToolOptions()
        ?: fromCommandLine()
        ?: "."

    internal fun fromProviders() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultParameterDefinitions.INSTALLATION_DIR.name] }
        .lastOrNull()

    @OptIn(ExperimentalForeignApi::class)
    private fun fromJavaToolOptions() = getenv("JAVA_TOOL_OPTIONS")?.toKString()?.let(::parse)

    private fun fromCommandLine() = runCatching(AgentProcessMetadata::commandLine::get).getOrNull()?.let(::parse)

    internal fun parse(value: String) = value.split("-agentpath:").drop(1).map(String::trim)
        .find { it.matches(agentLibPathRegex) }
        ?.let { agentLibPathRegex.matchEntire(it)!!.groupValues[1] }
        ?.let { it.takeIf("/"::equals) ?: it.removeSuffix("/").removeSuffix("\\").takeIf(String::isNotEmpty) ?: "." }

}
