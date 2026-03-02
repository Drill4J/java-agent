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
    private val _inputParameters: Map<String, String>
) : AgentConfiguration {

    actual override val parameters: AgentParameters = DefaultAgentParameters(_inputParameters)
    actual override val agentMetadata by lazy { agentMetadata() }

    actual val inputParameters: Map<String, String>
        get() = _inputParameters.toMap()

    private fun agentMetadata() = AgentMetadata(
        groupId = parameters[DefaultParameterDefinitions.GROUP_ID],
        appId = parameters[DefaultParameterDefinitions.APP_ID] ?: "",
        buildVersion = parameters[DefaultParameterDefinitions.BUILD_VERSION] ?: "",
        commitSha = parameters[DefaultParameterDefinitions.COMMIT_SHA] ?: "",
        envId = parameters[DefaultParameterDefinitions.ENV_ID] ?: "",
        instanceId = parameters[DefaultParameterDefinitions.INSTANCE_ID] ?: "",
        packagesPrefixes = parameters[DefaultParameterDefinitions.PACKAGE_PREFIXES] as List<String>
    )

}
