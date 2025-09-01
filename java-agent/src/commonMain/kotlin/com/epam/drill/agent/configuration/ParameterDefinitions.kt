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

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameterDefinitionCollection
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition

object ParameterDefinitions: AgentParameterDefinitionCollection() {

    val API_URL = AgentParameterDefinition.forString(
        name = "apiUrl",
        description = "URL to Drill4J Backend /api endpoint. Example: http://localhost:8090/api",
        parser = { if (!it.endsWith("/")) "$it/" else it  },
        validator = { validTransportUrl() }).register()
    val API_KEY = NullableAgentParameterDefinition.forString(
        name = "apiKey",
        description = "Drill4J API key. It is recommended to set it with DRILL_API_KEY env variable, rather than using command line argument"
    ).register()
    val MESSAGE_QUEUE_LIMIT = AgentParameterDefinition.forString(name = "messageQueueLimit", defaultValue = "512Mb").register()
    val MESSAGE_MAX_RETRIES = AgentParameterDefinition.forInt(name = "messageMaxRetries", defaultValue = Int.MAX_VALUE).register()
    val SSL_TRUSTSTORE = NullableAgentParameterDefinition.forString(name = "sslTruststore").register()
    val SSL_TRUSTSTORE_PASSWORD = NullableAgentParameterDefinition.forString(name = "sslTruststorePassword").register()
    val LOG_LEVEL = AgentParameterDefinition.forString(name = "logLevel", defaultValue = "INFO").register()
    val LOG_FILE = NullableAgentParameterDefinition.forString(name = "logFile").register()
    val LOG_LIMIT = AgentParameterDefinition.forInt(name = "logLimit", defaultValue = 512).register()
    val USE_PROTOBUF_SERIALIZER =
        AgentParameterDefinition.forBoolean(name = "useProtobufSerializer", defaultValue = true).register()
    val USE_GZIP_COMPRESSION = AgentParameterDefinition.forBoolean(name = "useGzipCompression", defaultValue = true).register()
    val IS_WS_MESSAGES = AgentParameterDefinition.forBoolean(name = "isWsMsg", defaultValue = true).register()
}
