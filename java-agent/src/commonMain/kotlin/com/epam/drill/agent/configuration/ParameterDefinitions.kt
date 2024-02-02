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

import com.epam.drill.common.agent.configuration.AgentParameterDefinition

object ParameterDefinitions {

    val ADMIN_ADDRESS = AgentParameterDefinition.forString(
        name = "adminAddress",
        parser = { it.takeIf(URL_SCHEME_REGEX::matches) ?: "https://$it"}
    )
    val API_KEY = AgentParameterDefinition.forString(name = "apiKey")
    val MESSAGE_QUEUE_LIMIT = AgentParameterDefinition.forString(name = "coverageRetentionLimit", defaultValue = "512Mb")
    val SSL_TRUSTSTORE = AgentParameterDefinition.forString(name = "sslTruststore")
    val SSL_TRUSTSTORE_PASSWORD = AgentParameterDefinition.forString(name = "sslTruststorePassword")
    val LOG_LEVEL = AgentParameterDefinition.forString(name = "logLevel", defaultValue = "INFO")
    val LOG_FILE = AgentParameterDefinition.forString(name = "logFile")
    val LOG_LIMIT = AgentParameterDefinition.forInt(name = "logLimit", defaultValue = 512)
    val REQUEST_PATTERN = AgentParameterDefinition.forString(name = "requestPattern", defaultValue = "drill-session-id")
    val HTTP_HOOK_ENABLED = AgentParameterDefinition.forBoolean(name = "httpHookEnabled", defaultValue = true)
    val IS_WEB_APP = AgentParameterDefinition.forBoolean(name = "isWebApp")
    val IS_KAFKA = AgentParameterDefinition.forBoolean(name = "isKafka")
    val IS_CADENCE = AgentParameterDefinition.forBoolean(name = "isCadence")
    val IS_TLS_APP = AgentParameterDefinition.forBoolean(name = "isTlsApp")
    val IS_ASYNC_APP = AgentParameterDefinition.forBoolean(name = "isAsyncApp")

    private val URL_SCHEME_REGEX = Regex("\\w+://.+")

}
