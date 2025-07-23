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
package com.epam.drill.agent

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.configuration.AgentMetadataValidator
import com.epam.drill.agent.configuration.DefaultAgentConfiguration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.logging.LoggingConfiguration
import com.epam.drill.agent.test2code.Test2Code
import com.epam.drill.agent.test2code.configuration.ParametersValidator
import com.epam.drill.agent.transport.HttpAgentMessageDestinationMapper
import com.epam.drill.agent.transport.JsonAgentMessageSerializer
import com.epam.drill.agent.transport.SimpleAgentMessageSender
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import java.io.File
import kotlin.takeIf

fun main(args: Array<String>) {
    LoggingConfiguration.readDefaultConfiguration()

    val argsMap: Map<String, String> = args
        .filter { it.startsWith("--") && it.contains("=") }
        .associate {
            val (key, value) = it.removePrefix("--").split("=", limit = 2)
            key to value
        }.filter { it.value.isNotEmpty() }
    val envMap = System.getenv()
        .filterKeys { it.startsWith("DRILL_") }
        .filterValues { !it.isNullOrEmpty() }
        .mapKeys { toParameterName(it) }
    val configuration = DefaultAgentConfiguration(envMap + argsMap)
    AgentMetadataValidator.validate(configuration.parameters)
    ParametersValidator.validate(configuration.parameters)

    val commitSha = configuration.parameters[DefaultParameterDefinitions.COMMIT_SHA]
    val buildVersion = configuration.parameters[DefaultParameterDefinitions.BUILD_VERSION]
    if (commitSha == null && buildVersion == null) {
        throw IllegalArgumentException("Either commitSha or buildVersion must be provided")
    }

    configuration.parameters[ParameterDefinitions.LOG_LEVEL].takeIf { it.isNotEmpty() }
        ?.let(LoggingConfiguration::setLoggingLevels)
    configuration.parameters[ParameterDefinitions.LOG_FILE].takeIf { it.isNotEmpty() }
        ?.let(LoggingConfiguration::setLoggingFilename)
    configuration.parameters[ParameterDefinitions.LOG_LIMIT].let(LoggingConfiguration::setLogMessageLimit)

    val transport = HttpAgentMessageTransport(
        serverAddress = configuration.parameters[ParameterDefinitions.API_URL],
        apiKey = configuration.parameters[ParameterDefinitions.API_KEY],
        sslTruststore = configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE].takeIf(String::isNotEmpty)
            ?.let { resolvePath(configuration, it) } ?: "",
        sslTruststorePass = configuration.parameters[ParameterDefinitions.SSL_TRUSTSTORE_PASSWORD],
        gzipCompression = configuration.parameters[ParameterDefinitions.USE_GZIP_COMPRESSION],
    )
    val serializer = JsonAgentMessageSerializer<AgentMessage>()
    val mapper = HttpAgentMessageDestinationMapper()
    val sender = SimpleAgentMessageSender(transport, serializer, mapper)
    val test2Code = Test2Code(
        id = "test2Code",
        agentContext = RequestAgentContext,
        sender = sender,
        configuration = configuration
    )
    sender.send(AgentMessageDestination("PUT", "builds"), configuration.agentMetadata)
    test2Code.scanAndSendMetadataClasses()
}

//TODO: duplicate with JvmModuleMessageSender
private fun resolvePath(configuration: AgentConfiguration, path: String) = File(path).run {
    val installationDir = File(configuration.parameters[DefaultParameterDefinitions.INSTALLATION_DIR] ?: "")
    val resolved = this.takeIf(File::exists)
        ?: this.takeUnless(File::isAbsolute)?.let(installationDir::resolve)
    resolved?.takeUnless(File::isDirectory)?.absolutePath ?: path
}

//TODO: duplicate from ValidatedParametersProvider
internal fun toParameterName(entry: Map.Entry<String, String>) = entry.key
    .removePrefix("DRILL_")
    .lowercase()
    .split("_")
    .joinToString("") { it.replaceFirstChar(Char::uppercase) }
    .replaceFirstChar(Char::lowercase)