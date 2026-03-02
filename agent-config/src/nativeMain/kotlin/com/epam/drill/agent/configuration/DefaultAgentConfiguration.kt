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

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentMetadata
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.AgentType

actual class DefaultAgentConfiguration(
    private val configurationProviders: Set<AgentConfigurationProvider>
) : AgentConfiguration {

    private val _inputParameters = inputParameters()

    actual override val parameters: AgentParameters = DefaultAgentParameters(_inputParameters).also(::defineDefaults)
    actual override val agentMetadata by lazy { agentMetadata() }

    actual val inputParameters: Map<String, String>
        get() = _inputParameters.toMap()

    private fun inputParameters() = configurationProviders
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

    @Suppress("UNCHECKED_CAST")
    private fun agentMetadata() = AgentMetadata(
        groupId = parameters[DefaultParameterDefinitions.GROUP_ID],
        appId = parameters[DefaultParameterDefinitions.APP_ID] ?: "",
        buildVersion = parameters[DefaultParameterDefinitions.BUILD_VERSION],
        commitSha = parameters[DefaultParameterDefinitions.COMMIT_SHA],
        envId = parameters[DefaultParameterDefinitions.ENV_ID],
        instanceId = parameters[DefaultParameterDefinitions.INSTANCE_ID] ?: "",
        packagesPrefixes = parameters[DefaultParameterDefinitions.PACKAGE_PREFIXES] as List<String>
    )

}
