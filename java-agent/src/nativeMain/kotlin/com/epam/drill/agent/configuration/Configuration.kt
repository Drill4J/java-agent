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

import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.toKString
import kotlinx.serialization.modules.serializersModuleOf
import platform.posix.getenv
import mu.KotlinLogging
import com.epam.drill.agent.SYSTEM_CONFIG_PATH
import com.epam.drill.agent.agentVersion
import com.epam.drill.agent.configuration.serialization.SimpleMapDecoder
import com.epam.drill.common.agent.configuration.AgentConfig
import com.epam.drill.common.agent.configuration.AgentType
import com.epam.drill.jvmapi.callObjectIntMethod
import com.epam.drill.jvmapi.callObjectStringMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.logging.LoggingConfiguration
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open

private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.Configuration")
private const val DRILL_INSTALLATION_DIR_PARAM = "drillInstallationDir"
private const val CONFIG_PATH_PARAM = "configPath"
internal val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

fun performInitialConfiguration(aa: AgentArguments) {
    adminAddress = URL(aa.adminAddress!!)
    agentConfig = AgentConfig(
        id = aa.agentId!!,
        instanceId = aa.instanceId,
        agentVersion = agentVersion,
        buildVersion = aa.buildVersion!!,
        serviceGroupId = aa.groupId,
        agentType = AgentType.JAVA
    )
    updateAgentParameters(aa.defaultParameters(), true)
}

fun updateAgentParameters(parameters: Map<String, AgentParameter>, initialization: Boolean = false) {
    agentParameters = agentParameters.copy(
        sslTruststore = parameters[AgentArguments::sslTruststore.name]?.value
            ?: agentParameters.sslTruststore,
        sslTruststorePassword = parameters[AgentArguments::sslTruststorePassword.name]?.value
            ?: agentParameters.sslTruststorePassword,
        classScanDelay = parameters[AgentArguments::classScanDelay.name]?.value
            ?.toLong()?.toDuration(DurationUnit.MILLISECONDS) ?: agentParameters.classScanDelay,
        packagePrefixes = parameters[AgentArguments::packagePrefixes.name]?.value ?: agentParameters.packagePrefixes,
        scanClassPath = parameters[AgentArguments::scanClassPath.name]?.value ?: agentParameters.scanClassPath,
        logLevel = parameters[AgentArguments::logLevel.name]?.value ?: agentParameters.logLevel,
        logFile = parameters[AgentArguments::logFile.name]?.value?.takeIf(String::isNotEmpty),
        logLimit = parameters[AgentArguments::logLimit.name]?.value?.toIntOrNull() ?: agentParameters.logLimit,
        isAsyncApp = parameters[AgentArguments::isAsyncApp.name]?.value.toBoolean(),
        isWebApp = parameters[AgentArguments::isWebApp.name]?.value.toBoolean(),
        isKafka = parameters[AgentArguments::isKafka.name]?.value.toBoolean(),
        isCadence = parameters[AgentArguments::isCadence.name]?.value.toBoolean(),
        isTlsApp = parameters[AgentArguments::isTlsApp.name]?.value.toBoolean(),
        coverageRetentionLimit = parameters[AgentArguments::coverageRetentionLimit.name]?.value
            ?: agentParameters.coverageRetentionLimit,
        sendCoverageInterval = parameters[AgentArguments::sendCoverageIntervalMs.name]?.value?.toLong()
            ?: agentParameters.sendCoverageInterval,
        drillInstallationDir = parameters[AgentArguments::drillInstallationDir.name]?.value
            ?: agentParameters.drillInstallationDir,
    )
    updateNativeLoggingConfiguration()
    if (!initialization) updateJvmLoggingConfiguration()
    logger.debug { "Agent parameters '$agentParameters' is initialized." }
}

fun defaultNativeLoggingConfiguration() {
    LoggingConfiguration.readDefaultConfiguration()
}

fun updateNativeLoggingConfiguration() {
    LoggingConfiguration.setLoggingLevels(agentParameters.logLevel)
    if (LoggingConfiguration.getLoggingFilename() != agentParameters.logFile) {
        LoggingConfiguration.setLoggingFilename(agentParameters.logFile)
    }
    if (LoggingConfiguration.getLogMessageLimit() != agentParameters.logLimit) {
        LoggingConfiguration.setLogMessageLimit(agentParameters.logLimit)
    }
}

fun defaultJvmLoggingConfiguration() {
    callObjectVoidMethod(LoggingConfiguration::class, LoggingConfiguration::readDefaultConfiguration)
}

fun updateJvmLoggingConfiguration() {
    callObjectVoidMethodWithString(LoggingConfiguration::class, "setLoggingLevels", agentParameters.logLevel)
    if (callObjectStringMethod(
            LoggingConfiguration::class,
            LoggingConfiguration::getLoggingFilename
        ) != agentParameters.logFile
    ) {
        callObjectVoidMethodWithString(
            LoggingConfiguration::class,
            LoggingConfiguration::setLoggingFilename,
            agentParameters.logFile
        )
    }
    if (callObjectIntMethod(
            LoggingConfiguration::class,
            LoggingConfiguration::getLogMessageLimit
        ) != agentParameters.logLimit
    ) {
        callObjectVoidMethodWithInt(
            LoggingConfiguration::class,
            LoggingConfiguration::setLogMessageLimit,
            agentParameters.logLimit
        )
    }
}

fun updatePackagePrefixesConfiguration() {
    agentConfig = agentConfig.copy(packagesPrefixes = agentParameters.packagePrefixes.split(";"))
}

fun idHeaderPairFromConfig(): Pair<String, String> = when (agentConfig.serviceGroupId) {
    "" -> "drill-agent-id" to agentConfig.id
    else -> "drill-group-id" to agentConfig.serviceGroupId
}

fun retrieveAdminUrl() = adminAddress?.toUrlString(false).toString()

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
        val url = URL(address)
        if (url.scheme == null)
            "wss://${address}"
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
    return getenv("JAVA_TOOL_OPTIONS")?.toKString() ?: runCatching { getAgentPathCommandFromProcess() }.getOrNull()
}

internal fun parseAgentDirFromAgentPathCommand(agentPathCommand: String, pathSeparator: String = "/"): String? {
    val getPathSansExtension = { input: String? -> input?.run {
        Regex("\\s*\"?(.+)(\\.so|\\.dll)").matchAt(this, 0)?.groups?.get(1)?.value
    }}
    return agentPathCommand
        .split("-agentpath:")
        .find { it.contains("drill_agent") }
        ?.let { getPathSansExtension(it) }
        ?.takeIf { it.contains(pathSeparator) }
        ?.substringBeforeLast(pathSeparator)
}
