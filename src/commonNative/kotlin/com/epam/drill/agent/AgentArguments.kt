/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent

import com.benasher44.uuid.*
import com.epam.drill.logger.api.*
import kotlinx.serialization.*

@Serializable
data class AgentArguments(
    val agentId: String,
    val adminAddress: String,
    val drillInstallationDir: String = javaProcess().firstAgentPath,
    val buildVersion: String = "unspecified",
    val instanceId: String = uuid4().toString(),
    val groupId: String = "",
    val logLevel: String = LogLevel.ERROR.name,
    val logFile: String? = null,
    val isWebApp: Boolean = false,
    val isTlsApp: Boolean = false,
    val isAsyncApp: Boolean = false,
    val webAppNames: String = "",
    val classScanDelay: Long = 0L
) {
    val webApps: List<String>
        get() = webAppNames.split(":", ",")
}
