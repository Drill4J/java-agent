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
import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.configuration.AgentMetadata
import com.epam.drill.common.agent.configuration.AgentParameters

actual object JavaAgentConfiguration : AgentConfiguration {

    private val logger = KotlinLogging.logger {}
    private lateinit var configuration: DefaultAgentConfiguration

    actual override val agentMetadata: AgentMetadata
        get() = configuration.agentMetadata

    actual override val parameters: AgentParameters
        get() = configuration.parameters

    actual fun initializeNative(agentOptions: String): Unit = throw NotImplementedError()

    actual fun initializeJvm(inputParameters: String) {
        val parameters = inputParameters.split(",")
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
        logger.debug { "initializeJvm: Found input parameters: $parameters" }
        configuration = DefaultAgentConfiguration(parameters)
    }

}
