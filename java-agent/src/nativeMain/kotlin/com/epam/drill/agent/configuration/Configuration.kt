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
import com.epam.drill.adminAddress
import com.epam.drill.agentConfig
import com.epam.drill.agentConfigUpdater
import com.epam.drill.drillInstallationDir
import com.epam.drill.agent.SYSTEM_JAVA_APP_JAR
import com.epam.drill.agent.agentVersion
import com.epam.drill.agent.configuration.serialization.SimpleMapDecoder
import com.epam.drill.common.AgentConfig
import com.epam.drill.common.AgentConfigUpdater
import com.epam.drill.common.AgentParameter
import com.epam.drill.common.AgentType
import com.epam.drill.common.PackagesPrefixes
import com.epam.drill.common.ws.URL
import com.epam.drill.core.setPackagesPrefixes
import com.epam.drill.jvmapi.callObjectIntMethod
import com.epam.drill.jvmapi.callObjectStringMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.logging.LoggingConfiguration

private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.Configuration")

fun performInitialConfiguration(initialParams: Map<String, String>) {
    val agentArguments = initialParams.parseAs<AgentArguments>()
    agentArguments.let { aa ->
        drillInstallationDir = aa.drillInstallationDir
        adminAddress = URL("ws://${aa.adminAddress}")
        agentConfig = AgentConfig(
            id = aa.agentId,
            instanceId = aa.instanceId,
            agentVersion = agentVersion,
            buildVersion = aa.buildVersion ?: calculateBuildVersion() ?: "unspecified",
            serviceGroupId = aa.groupId,
            agentType = AgentType.JAVA,
            parameters = aa.defaultParameters(),
        )
        updateAgentParameters(agentConfig.parameters, true)
        agentConfigUpdater = object : AgentConfigUpdater {
            override fun updateParameters(config: AgentConfig) = updateAgentParameters(config.parameters)
        }
    }
}

fun updateAgentParameters(parameters: Map<String, AgentParameter>, initialization: Boolean = false) {
    agentParameters = agentParameters.copy(
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
    logger.debug { "after update configs by params: config '$agentParameters'" }
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
    setPackagesPrefixes(PackagesPrefixes(agentParameters.packagePrefixes.split(";")))
}

private inline fun <reified T : Any> Map<String, String>.parseAs(): T = run {
    val serializer = T::class.serializer()
    val module = serializersModuleOf(serializer)
    serializer.deserialize(SimpleMapDecoder(module, this))
}

private fun calculateBuildVersion(): String? = runCatching {
    getenv(SYSTEM_JAVA_APP_JAR)?.toKString()?.let {
        "(.*)/(.*).jar".toRegex().matchEntire(it)?.let { matchResult ->
            if (matchResult.groupValues.size == 3) {
                val buildVersion = matchResult.groupValues[2]
                logger.debug { "calculated build version = '$buildVersion'" }
                buildVersion
            } else {
                null
            }
        }
    }
}.getOrNull()
