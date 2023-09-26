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

import com.epam.drill.agent.SYSTEM_CONFIG_PATH
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.toKString
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import platform.posix.getenv
import mu.KotlinLogging
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
import com.epam.drill.transport.URL
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*

private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.Configuration")

fun performInitialConfiguration(aa: AgentArguments) {
    drillInstallationDir = aa.drillInstallationDir
    adminAddress = URL("ws://${aa.adminAddress}")
    agentConfig = AgentConfig(
        id = aa.agentId!!,
        instanceId = aa.instanceId ?: "",
        agentVersion = agentVersion,
        buildVersion = aa.buildVersion!!,
        serviceGroupId = aa.groupId ?: "",
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
    logger.info { "Agent parameters '$agentParameters' is initialized." }
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

fun convertToAgentArguments(options: String): AgentArguments {
    logger.debug { "agent options:$options" }
    val agentParameters = options.asAgentParams()
    val configPath = agentParameters["configPath"] ?: getenv(SYSTEM_CONFIG_PATH)?.toKString()
    logger.debug { "configFile=$configPath, agent parameters:$agentParameters" }
    val agentParams = if (!configPath.isNullOrEmpty()) {
        val properties = readFile(configPath)
        logger.debug { "properties file:$properties" }
        properties.asAgentParams("\n", "#")
    } else {
        logger.warn { "Deprecated. You should use a config file instead of options. It will be removed in the next release" }
        agentParameters
    }
    logger.debug { "result of agent parameters:$agentParams" }
    return parseAsAgentArguments(agentParams)
}

private fun parseAsAgentArguments(map: Map<String, String>) = AgentArguments::class.serializer().run {
    val module = serializersModuleOf(this)
    this.deserialize(SimpleMapDecoder(module, map))
}
