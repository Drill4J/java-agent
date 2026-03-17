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
package com.epam.drill.agent.transport.http

import java.lang.UnsupportedOperationException
import com.epam.drill.agent.transport.AgentMessageDestinationMapper
import com.epam.drill.agent.common.transport.AgentMessageDestination

class HttpAutotestAgentMessageDestinationMapper(
    private val groupId: String,
    private val jsAgentId: String,
    private val jsAgentBuildVersion: String,
) : AgentMessageDestinationMapper {

    override fun map(destination: AgentMessageDestination): AgentMessageDestination {
        val target = when(destination.target) {
            // TODO configurations w/o JavaScript agent
            "raw-javascript-coverage" -> "/api/groups/${groupId}/agents/$jsAgentId/builds/$jsAgentBuildVersion/raw-javascript-coverage"
            "tests-metadata" -> "/api/tests-metadata"
            else -> throw UnsupportedOperationException(
                "HttpAutotestAgentMessageDestinationMapper does not support target ${destination.target}")
        }
        return destination.copy(target = target)
    }
}
