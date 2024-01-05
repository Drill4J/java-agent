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

import com.epam.drill.agent.configuration.JavaAgentConfiguration
import com.epam.drill.agent.configuration.ParameterDefinitions

actual object HeadersRetriever {

    internal val requestPattern = JavaAgentConfiguration.parameters[ParameterDefinitions.REQUEST_PATTERN]

    internal val adminAddress = JavaAgentConfiguration.parameters[ParameterDefinitions.ADMIN_ADDRESS]
        .let { Regex("\\w+://(.+)").matchEntire(it)!!.groupValues[1] }

    internal val idHeaderPair = JavaAgentConfiguration.agentMetadata.serviceGroupId.takeIf(String::isNotEmpty)
        ?.let { "drill-group-id" to JavaAgentConfiguration.agentMetadata.serviceGroupId }
        ?: let { "drill-agent-id" to JavaAgentConfiguration.agentMetadata.id }

    actual fun adminAddressHeader(): String? = "drill-admin-url"

    actual fun retrieveAdminAddress(): String? = adminAddress

    actual fun sessionHeaderPattern(): String? = requestPattern

    actual fun idHeaderConfigKey(): String? = idHeaderPair.first

    actual fun idHeaderConfigValue(): String? = idHeaderPair.second

}
