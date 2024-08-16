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

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import mu.KotlinLogging
import com.epam.drill.agent.configuration.provider.AgentOptionsProvider
import com.epam.drill.agent.configuration.provider.EnvironmentVariablesProvider
import com.epam.drill.agent.configuration.provider.InstallationDirProvider
import com.epam.drill.agent.configuration.provider.PropertiesFileProvider
import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.configuration.AgentParameters
import com.epam.drill.jvmapi.callObjectVoidMethodWithString

actual object Configuration : AgentConfiguration {

    private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.JavaAgentConfiguration")
    private val configuration = AtomicReference<DefaultAgentConfiguration?>(null)

    actual override val agentMetadata: AgentMetadata
        get() = configuration.value!!.agentMetadata

    actual override val parameters: AgentParameters
        get() = configuration.value!!.parameters

    actual fun initializeNative(agentOptions: String) {
        val environmentVariablesProvider = EnvironmentVariablesProvider()
        logger.debug { "initializeNative: Found environment variables: ${environmentVariablesProvider.configuration}" }
        val agentOptionsProvider = AgentOptionsProvider(agentOptions)
        logger.debug { "initializeNative: Found agent options: ${agentOptionsProvider.configuration}" }
        val installationDirProvider = InstallationDirProvider(setOf(
            environmentVariablesProvider,
            agentOptionsProvider
        ))
        logger.debug { "initializeNative: Found installation dir: ${installationDirProvider.configuration}" }
        val propertiesFileProvider = PropertiesFileProvider(setOf(
            environmentVariablesProvider,
            agentOptionsProvider,
            installationDirProvider
        ))
        logger.debug { "initializeNative: Found from properties file: ${propertiesFileProvider.configuration}" }
        val validatedParametersProvider = ValidatedParametersProvider(setOf(
            environmentVariablesProvider,
            agentOptionsProvider,
            installationDirProvider,
            propertiesFileProvider
        ))
        val runtimeParametersProvider = RuntimeParametersProvider()
        configuration.value = DefaultAgentConfiguration(setOf(
            validatedParametersProvider,
            environmentVariablesProvider,
            agentOptionsProvider,
            installationDirProvider,
            propertiesFileProvider,
            runtimeParametersProvider
        )).freeze()
    }

    actual fun initializeJvm(inputParameters: String): Unit =
        callObjectVoidMethodWithString(Configuration::class, "initializeJvm", inputParameters)

    fun initializeJvm(): Unit = configuration.value!!.inputParameters.entries
        .joinToString(",") { "${it.key}=${it.value}" }
        .let(::initializeJvm)

}
