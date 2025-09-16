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

import mu.KotlinLogging
import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentMetadata
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.configuration.provider.AgentOptionsProvider
import com.epam.drill.agent.configuration.provider.EnvironmentVariablesProvider

actual object Configuration : AgentConfiguration {

    private val logger = KotlinLogging.logger {}
    private lateinit var configuration: DefaultAgentConfiguration

    private fun inputParameters(configurationProviders: Set<AgentConfigurationProvider>) = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .map(AgentConfigurationProvider::configuration)
        .reduce { acc, map -> acc + map }

    private fun defineDefaults(agentParameters: AgentParameters)  {
        agentParameters.define(
            DefaultParameterDefinitions.APP_ID,
            DefaultParameterDefinitions.INSTANCE_ID,
            DefaultParameterDefinitions.BUILD_VERSION,
            DefaultParameterDefinitions.GROUP_ID,
            DefaultParameterDefinitions.COMMIT_SHA,
            DefaultParameterDefinitions.ENV_ID,
            DefaultParameterDefinitions.INSTALLATION_DIR,
            DefaultParameterDefinitions.CONFIG_PATH
        )
        agentParameters.define(DefaultParameterDefinitions.PACKAGE_PREFIXES)
    }

    actual override val agentMetadata: AgentMetadata
        get() = configuration.agentMetadata

    actual override val parameters: AgentParameters
        get() = configuration.parameters

    actual fun initializeNative(agentOptions: String) {
        val environmentVariablesProvider = EnvironmentVariablesProvider()
        logger.debug { "initializeNative: Found environment variables: ${environmentVariablesProvider.configuration}" }
        val agentOptionsProvider = AgentOptionsProvider(agentOptions)
        logger.debug { "initializeNative: Found agent options: ${agentOptionsProvider.configuration}" }
        val validatedParametersProvider = ValidatedParametersProvider(setOf(
            environmentVariablesProvider,
            agentOptionsProvider
        ))
        val runtimeParametersProvider = RuntimeParametersProvider()

        val inputParameters = inputParameters(setOf(
            validatedParametersProvider,
            environmentVariablesProvider,
            agentOptionsProvider,
            runtimeParametersProvider
        ))
        configuration = DefaultAgentConfiguration(inputParameters)
        defineDefaults(configuration.parameters)
        logger.debug { "initializeNative: Final input parameters: ${configuration.inputParameters}" }
    }

    actual fun initializeJvm(inputParameters: String) {
        val parameters = inputParameters.split(",")
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
        logger.debug { "initializeJvm: Found input parameters: $parameters" }
        configuration = DefaultAgentConfiguration(parameters)
    }

}
