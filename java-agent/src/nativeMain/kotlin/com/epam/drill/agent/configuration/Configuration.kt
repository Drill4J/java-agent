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

import kotlinx.cinterop.toKString
import kotlinx.serialization.modules.serializersModuleOf
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.getenv
import platform.posix.open
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input
import mu.KotlinLogging
import com.epam.drill.agent.SYSTEM_CONFIG_PATH
import com.epam.drill.agent.configuration.serialization.SimpleMapDecoder

private const val DRILL_INSTALLATION_DIR_PARAM = "drillInstallationDir"
private const val CONFIG_PATH_PARAM = "configPath"

private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.Configuration")
private val urlSchemeRegex = Regex("\\w+://(.*)")
internal val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

fun convertToAgentArguments(args: String): AgentArguments {
    val commandLineParams = parseArguments(args)
    logger.info { "command line parameters: $commandLineParams" }

    val drillInstallationDir = getDrillInstallationDir(commandLineParams)
    logger.info { "drillInstallationDir: $drillInstallationDir" }

    val configFilePath = getConfigFilePath(commandLineParams, drillInstallationDir)
    logger.info { "config path: $configFilePath" }

    val configFileParams = getConfigFileParams(configFilePath)
    logger.info { "config file parameters: $configFileParams" }

    val resultParams = mutableMapOf<String, String>()
        .apply { putAll(configFileParams) }
        .apply { putAll(commandLineParams) }
        .apply { put(DRILL_INSTALLATION_DIR_PARAM, drillInstallationDir) }
    logger.info { "result parameters: $resultParams" }

    return resultParams.toAgentArguments()
}

fun Map<String, String>.toAgentArguments(): AgentArguments {
    val serializer = AgentArguments.serializer()
    val module = serializersModuleOf(serializer)
    return serializer.deserialize(SimpleMapDecoder(module, this))
}

fun validate(args: AgentArguments) {
    args.adminAddress = addWsSchema(args.adminAddress)
    AgentArgumentsValidator.validate(args)
}

private fun addWsSchema(address: String?): String? {
    if (address == null) return null
    return try {
        if (!address.matches(urlSchemeRegex))
            "https://${address}"
        else
            address
    } catch (ignore: RuntimeException) {
        address
    }
}


private fun readFile(filepath: String): String {
    val fileDescriptor = open(filepath, O_RDONLY)
    return fileDescriptor
        .takeIf { it != -1 }
        ?.let { Input(it).readText().also { close(fileDescriptor) } }
        ?: "".also { logger.error { "Cannot open the config file with path=$filepath" } }
}

private fun parseArguments(
    input: String?,
    filterPrefix: String = "",
    mapDelimiter: String = "=",
    lineDelimiters: Array<String> = arrayOf(",")
): Map<String, String> {
    if (input.isNullOrEmpty()) return emptyMap()
    try {
        return input.split(*lineDelimiters)
            .filter { it.isNotEmpty() && (filterPrefix.isEmpty() || !it.startsWith(filterPrefix)) }
            .associate { it.substringBefore(mapDelimiter) to it.substringAfter(mapDelimiter, "").trim() }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $input")
    }
}

private fun getConfigFileParams(configFilePath: String): Map<String, String> {
    return configFilePath
        .runCatching(::readFile)
        .getOrNull()
        .let { parseArguments(it, filterPrefix = "#", lineDelimiters = arrayOf("\n\r", "\r\n", "\n", "\r")) }
}

private fun getConfigFilePath(
    commandLineParams: Map<String, String>,
    drillInstallationDir: String
) = (commandLineParams[CONFIG_PATH_PARAM]
    ?: getenv(SYSTEM_CONFIG_PATH)?.toKString()
    ?: "${drillInstallationDir}${pathSeparator}drill.properties")

private fun getDrillInstallationDir(commandLineParams: Map<String, String>): String {
    return commandLineParams[DRILL_INSTALLATION_DIR_PARAM]
        ?: getAgentPathCommand()?.let { parseAgentDirFromAgentPathCommand(it, pathSeparator) }
        ?: "."
}

private fun getAgentPathCommand(): String? {
    return getenv("JAVA_TOOL_OPTIONS")?.toKString() ?: runCatching { AgentProcessMetadata.commandLine }.getOrNull()
}

internal fun parseAgentDirFromAgentPathCommand(agentPathCommand: String, pathSeparator: String = "/"): String? {
    val getPathSansExtension = { input: String? -> input?.run {
        Regex("\\s*\"?(.+)(\\.so|\\.dll)").matchAt(this, 0)?.groupValues?.get(1)
    }}
    return agentPathCommand
        .split("-agentpath:")
        .find { it.contains("drill_agent") }
        ?.let { getPathSansExtension(it) }
        ?.takeIf { it.contains(pathSeparator) }
        ?.substringBeforeLast(pathSeparator)
}
