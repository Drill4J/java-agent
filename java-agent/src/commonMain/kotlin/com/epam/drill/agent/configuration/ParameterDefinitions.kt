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
    val MESSAGE_SENDING_MODE = AgentParameterDefinition.forString(
        name = "messageSendingMode",
        description = "Message sending mode. Possible values: DIRECT, QUEUED",
        defaultValue = "QUEUED").register()
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

    val TEST_AGENT_ENABLED = AgentParameterDefinition.forBoolean(name = "testAgentEnabled", defaultValue = false).register()

    val WITH_JS_COVERAGE = AgentParameterDefinition.forBoolean(name = "withJsCoverage", defaultValue = false).register()
    val PROXY_ADDRESS = AgentParameterDefinition.forString(name = "browserProxyAddress", defaultValue = "").register()
    val DEVTOOLS_PROXY_ADDRESS = AgentParameterDefinition.forString(
        name = "devToolsProxyAddress",
        defaultValue = "http://localhost:9222",//TODO
        parser = { it.trim().takeIf(String::isBlank) ?: it.takeIf(URL_SCHEME_REGEX::matches) ?: "http://$it"}
    ).register()
    val DEVTOOLS_REPLACE_LOCALHOST = AgentParameterDefinition.forString(name = "devtoolsAddressReplaceLocalhost", defaultValue = "").register()
    val SESSION_ID = NullableAgentParameterDefinition.forString(name = "sessionId").register()
    val LAUNCH_TYPE = AgentParameterDefinition.forString(name = "launchType", defaultValue = "").register()
    val FRAMEWORK_PLUGINS = AgentParameterDefinition.forType(
        name = "rawFrameworkPlugins",
        defaultValue = emptyList(),
        parser = { it.split(";") }
    ).register()

    private val URL_SCHEME_REGEX = Regex("\\w+://.+")

    val JS_AGENT_BUILD_VERSION = NullableAgentParameterDefinition.forString(name = "jsAgentBuildVersion").register()
    val JS_AGENT_ID = NullableAgentParameterDefinition.forString(name = "jsAgentId").register()

    val TEST_TASK_ID = AgentParameterDefinition.forString(name = "testTaskId", defaultValue = "").register()
    val RECOMMENDED_TESTS_ENABLED = AgentParameterDefinition.forBoolean(name = "recommendedTestsEnabled", defaultValue = false).register()
    val RECOMMENDED_TESTS_COVERAGE_PERIOD_DAYS = AgentParameterDefinition.forInt(name = "recommendedTestsCoveragePeriodDays", defaultValue = 0).register()
    val RECOMMENDED_TESTS_TARGET_APP_ID = AgentParameterDefinition.forString(name = "recommendedTestsTargetAppId", defaultValue = "").register()
    val RECOMMENDED_TESTS_TARGET_COMMIT_SHA = AgentParameterDefinition.forString(name = "recommendedTestsTargetCommitSha", defaultValue = "").register()
    val RECOMMENDED_TESTS_TARGET_BUILD_VERSION = AgentParameterDefinition.forString(name = "recommendedTestsTargetBuildVersion", defaultValue = "").register()
    val RECOMMENDED_TESTS_BASELINE_COMMIT_SHA = AgentParameterDefinition.forString(name = "recommendedTestsBaselineCommitSha", defaultValue = "").register()
    val RECOMMENDED_TESTS_BASELINE_BUILD_VERSION = AgentParameterDefinition.forString(name = "recommendedTestsBaselineBuildVersion", defaultValue = "").register()
    val RECOMMENDED_TESTS_USE_MATERIALIZED_VIEWS = AgentParameterDefinition.forString(name = "recommendedTestsUseMaterializedViews", defaultValue = "").register()

    val TEST_TRACING_ENABLED = AgentParameterDefinition.forBoolean(name = "testTracingEnabled", defaultValue = true).register()
    val TEST_LAUNCH_METADATA_SENDING_ENABLED = AgentParameterDefinition.forBoolean(name = "testLaunchMetadataSendingEnabled", defaultValue = true).register()
}
