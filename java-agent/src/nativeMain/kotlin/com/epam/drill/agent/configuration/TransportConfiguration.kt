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

import kotlinx.serialization.protobuf.ProtoBuf
import com.epam.drill.common.agent.configuration.AgentConfig

actual object TransportConfiguration {

    actual fun getAgentConfigBytes() = ProtoBuf.encodeToByteArray(AgentConfig.serializer(), agentConfig)

    actual fun getAgentId() = agentConfig.id

    actual fun getBuildVersion() = agentConfig.buildVersion

    actual fun getSslTruststore() = agentParameters.sslTruststore

    actual fun getSslTruststorePassword() = agentParameters.sslTruststorePassword

    actual fun getDrillInstallationDir() = agentParameters.drillInstallationDir

    actual fun getCoverageRetentionLimit() = agentParameters.coverageRetentionLimit

    actual fun getAdminAddress() = adminAddress

}
