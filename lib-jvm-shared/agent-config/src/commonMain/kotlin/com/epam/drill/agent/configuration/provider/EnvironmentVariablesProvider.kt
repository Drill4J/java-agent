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
package com.epam.drill.agent.configuration.provider

import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.AgentProcessMetadata

class EnvironmentVariablesProvider(
    override val priority: Int = 500
) : AgentConfigurationProvider {

    override val configuration = configuration()

    private fun configuration() = runCatching(AgentProcessMetadata::environmentVars::get).getOrNull()
        ?.let(::parseKeys)
        ?: emptyMap()

    internal fun parseKeys(vars: Map<String, String>) = vars
        .filterKeys { it.startsWith("DRILL_") }
        .mapKeys(::toParameterName)

    internal fun toParameterName(entry: Map.Entry<String, String>) = entry.key
        .removePrefix("DRILL_")
        .lowercase()
        .split("_")
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }
        .replaceFirstChar(Char::lowercase)

}
