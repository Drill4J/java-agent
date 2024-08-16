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
package com.epam.drill.agent.request

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.common.agent.request.HeadersRetriever

actual object HeadersRetriever : HeadersRetriever {

    private val adminAddress = Configuration.parameters[ParameterDefinitions.DRILL_API_URL]

    private val agentIdHeader = Configuration.agentMetadata.groupId.takeIf(String::isNotEmpty)
        ?.let { "drill-group-id" to Configuration.agentMetadata.groupId }
        ?: let { "drill-agent-id" to Configuration.agentMetadata.appId }

    actual override fun adminAddressHeader() = "drill-admin-url"

    actual override fun adminAddressValue() = adminAddress

    actual override fun sessionHeader() = "drill-session-id"

    actual override fun agentIdHeader() = agentIdHeader.first

    actual override fun agentIdHeaderValue() = agentIdHeader.second

}
