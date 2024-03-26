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

import com.benasher44.uuid.uuid4
import com.epam.drill.agent.agentVersion

class RuntimeParametersProvider(
    override val priority: Int = 100
) : AgentConfigurationProvider {

    override val configuration = configuration()

    private fun configuration() = mapOf(
        Pair(DefaultParameterDefinitions.INSTANCE_ID.name, uuid4().toString()),
        Pair(DefaultParameterDefinitions.AGENT_VERSION.name, agentVersion)
    )

}
