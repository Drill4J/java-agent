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
import kotlinx.serialization.serializer
import platform.posix.getenv
import mu.KotlinLogging
import com.epam.drill.agent.SYSTEM_CONFIG_PATH
import com.epam.drill.agent.agentVersion
import com.epam.drill.agent.configuration.serialization.SimpleMapDecoder
import com.epam.drill.common.agent.configuration.AgentConfig
import com.epam.drill.common.agent.configuration.AgentParameter
import com.epam.drill.common.agent.configuration.AgentType
import com.epam.drill.common.agent.configuration.PackagesPrefixes
import com.epam.drill.jvmapi.callObjectIntMethod
import com.epam.drill.jvmapi.callObjectStringMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.logging.LoggingConfiguration
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import platform.posix.EROFS
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open

private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.Configuration")

fun performInitialConfiguration(aa: AgentArguments) {
    adminAddress = URL("ws://${aa.adminAddress}")
    agentConfig = AgentConfig(
        id = aa.agentId!!,
        instanceId = aa.instanceId,
        agentVersion = agentVersion,
        buildVersion = aa.buildVersion!!,
        serviceGroupId = aa.groupId,
        agentType = AgentType.JAVA,
        parameters = aa.defaultParameters()
    )
    updateAgentParameters(agentConfig.parameters, true)
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
        isTlsApp = parameters[AgentArguments::isTlsApp.name]?.value.toBoolean()
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
    if (callObjectStringMethod(LoggingConfiguration::class, LoggingConfiguration::getLoggingFilename) != agentParameters.logFile) {
        callObjectVoidMethodWithString(LoggingConfiguration::class, LoggingConfiguration::setLoggingFilename, agentParameters.logFile)
    }
    if (callObjectIntMethod(LoggingConfiguration::class, LoggingConfiguration::getLogMessageLimit) != agentParameters.logLimit) {
        callObjectVoidMethodWithInt(LoggingConfiguration::class, LoggingConfiguration::setLogMessageLimit, agentParameters.logLimit)
    }
}

fun updatePackagePrefixesConfiguration() {
    agentConfig = agentConfig.copy(packagesPrefixes = PackagesPrefixes(agentParameters.packagePrefixes.split(";")))
}

fun idHeaderPairFromConfig(): Pair<String, String> = when (agentConfig.serviceGroupId) {
    "" -> "drill-agent-id" to agentConfig.id
    else -> "drill-group-id" to agentConfig.serviceGroupId
}

fun retrieveAdminUrl() = adminAddress?.toUrlString(false).toString()

fun convertToAgentArguments(options: String) = parseAsAgentArguments(agentParams(options))

fun validate(args: AgentArguments) {
    AgentArgumentsValidator.validate(args)
}

private val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

private fun agentParams(options: String): Map<String, String> {
    logger.info { "agent options: $options" }
    val agentParams = asAgentParams(options)
    logger.info { "agent parameters: $agentParams" }
    val configPath = agentParams["configPath"]
        ?: getenv(SYSTEM_CONFIG_PATH)?.toKString()
        ?: "${drillInstallationDir}${pathSeparator}drill.properties"
    logger.info { "config path: $configPath" }
    val configParams = configPath.takeIf(String::isNotEmpty)
        ?.runCatching(::readFile)
        ?.getOrNull()
        ?.let { asAgentParams(it, "\n", "#") }
    logger.info { "config parameters: $configParams" }
    val resultParams = configParams?.toMutableMap()?.also { it.putAll(agentParams) } ?: agentParams
    logger.info { "result parameters: $resultParams" }
    return resultParams
}

private fun readFile(filepath: String): String {
    val isFilename: (String) -> Boolean = { Regex("[\\w\\-.]+").matches(it) }
    var fileDescriptor = open(filepath, O_RDONLY)
    if (fileDescriptor == -1 && isFilename(filepath))
        fileDescriptor = open(drillInstallationDir + pathSeparator + filepath, EROFS)
    if (fileDescriptor == -1)
        throw IllegalArgumentException("Cannot open the config file with path=$drillInstallationDir, file=$filepath")
    return Input(fileDescriptor).readText().also { close(fileDescriptor) }
}

private fun asAgentParams(
    input: String?,
    lineDelimiter: String = ",",
    filterPrefix: String = "",
    mapDelimiter: String = "="
): Map<String, String> {
    if (input.isNullOrEmpty()) return emptyMap()
    try {
        return input.split(lineDelimiter)
            .filter { it.isNotEmpty() && (filterPrefix.isEmpty() || !it.startsWith(filterPrefix)) }
            .associate { it.substringBefore(mapDelimiter) to it.substringAfter(mapDelimiter, "") }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $input")
    }
}

private fun parseAsAgentArguments(map: Map<String, String>) = AgentArguments::class.serializer().run {
    val module = serializersModuleOf(this)
    this.deserialize(SimpleMapDecoder(module, map))
}
