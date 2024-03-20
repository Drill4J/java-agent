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
package com.epam.drill.agent.transport

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.common.agent.transport.AgentMessageDestination

class HttpAgentMessageDestinationMapperTest {

    private lateinit var mapper: HttpAgentMessageDestinationMapper

    private val groupId = "someGroupId"
    private val agentId = "someAgentId"
    private val buildVersion = "someBuildVer"
    private val instanceId = "someInstanceId"

    @BeforeTest
    fun prepareConfiguration() {
        Configuration.initializeJvm("groupId=${groupId},agentId=${agentId},buildVersion=${buildVersion},instanceId=${instanceId}")
        mapper = HttpAgentMessageDestinationMapper()
    }

    @Test
    fun `map AgentMetadata`() {
        val destination = AgentMessageDestination("PUT", "")
        val mapped = mapper.map(destination)
        assertEquals(
            "/api/groups/${groupId}/agents/${agentId}/builds/${buildVersion}/instances/${instanceId}",
            mapped.target
        )
        assertEquals("PUT", mapped.type)
    }

    @Test
    fun `map AgentMessage`() {
        val destination = AgentMessageDestination("POST", "something-else")
        val mapped = mapper.map(destination)
        assertEquals(
            "/api/groups/${groupId}/agents/${agentId}/builds/${buildVersion}/instances/${instanceId}/something-else",
            mapped.target
        )
        assertEquals("POST", mapped.type)
    }

}
