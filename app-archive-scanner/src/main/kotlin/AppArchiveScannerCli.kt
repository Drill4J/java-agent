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
package com.epam.drill.agent.archive.scanner

import com.epam.drill.agent.common.configuration.AgentMetadata
import com.epam.drill.agent.test2code.classloading.ClassLoadersScanner
import com.epam.drill.agent.test2code.classparsing.parseAstClass
import com.epam.drill.agent.test2code.common.api.AstMethod
import com.epam.drill.agent.test2code.common.transport.ClassMetadata
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import kotlinx.serialization.json.Json
import kotlinx.cli.*
import java.util.UUID

fun main(args: Array<String>) {
    val parser = ArgParser("Drill4J JVM App Archive Scanner")

    val scanPaths by parser.option(
        ArgType.String,
        fullName = "appArchivePath",
        description = "Path to JAR/WAR/EAR to scan"
    ).required()

    val packagePrefixesStr by parser.option(
        ArgType.String,
        fullName = "packagePrefixes",
        description = """
            Packages starting with matching string will be scanned.
            It's usually set to the topmost common package of your application.

            Syntax:
            1. Parts of package names are separated with forward slashes / (and not dots .)
            2. Multiple packages can be specified using ; delimiter
            3. To exclude a package use ! before package name.

            Examples:
            my/org/some/cool/app;
            my/org/some/cool/app;!my/org/some/cool/app/dto
            my/org/some/cool/app;my/org/some/dependency

            Documentation:
            https://drill4j.github.io/docs/agents/java-agent/#how-to-set-package-prefixes
        """.trimIndent()
    ).required()

    val apiUrl by parser.option(
        ArgType.String,
        fullName = "apiUrl",
        description = "URL to Drill4J Admin /api endpoint. Example: http://localhost/api"
    ).required()

    val groupId by parser.option(
        ArgType.String,
        fullName = "groupId",
        description = "Unique arbitrary string identifying your application group. Example: my-cool-app"
    ).required()

    val appId by parser.option(
        ArgType.String,
        fullName = "appId",
        description = "Unique arbitrary string identifying your application. Example: api-service"
    ).required()

    val buildVersion by parser.option(
        ArgType.String,
        fullName = "buildVersion",
        description = "Build version of your application. Typically set to version tag. Example: v1.2.3"
    )

    val commitSha by parser.option(
        ArgType.String,
        fullName = "commitSha",
        description = "Full SHA hash of commit from which your application .jar is built. Example: 8d87b0c2379a925f2f5f4d85c731c8e77d9f2b3c"
    )

    val envId by parser.option(
        ArgType.String,
        fullName = "envId",
        description = "Environment identifier in which the application is running. Example: develop"
    )

    val apiKeyFromCmd by parser.option(
        ArgType.String,
        fullName = "apiKey",
        description = "Drill4J API key. It is recommended to set it with DRILL_API_KEY env variable, rather than using command line argument"
    )

    val verbose by parser.option(
        ArgType.Boolean,
        fullName = "verbose",
        description = "Enable verbose output"
    ).default(false)

    parser.parse(args)

    if (verbose) println("Verbose mode enabled")

    val packagePrefixes = packagePrefixesStr.split(";")

    val apiKey = System.getenv("DRILL_API_KEY")

    if (apiKey.isNullOrBlank()) {
        throw IllegalStateException("DRILL_API_KEY environment variable is either not set or set to a blank value")
    }
println("""
    scanPaths=${listOf(scanPaths)},
    packagePrefixes=$packagePrefixes,
    verbose=$verbose,
    apiUrl=$apiUrl,
    apiKey=$apiKey,
    groupId=$groupId,
    appId=$appId,
    buildVersion=$buildVersion,
    commitSha=$commitSha,
    envId=$envId
""".trimIndent())

    run(
        scanPaths = listOf(scanPaths),
        packagePrefixes = packagePrefixes,
        verbose = verbose,
        apiUrl = apiUrl,
        apiKey = apiKey,
        groupId = groupId,
        appId = appId,
        buildVersion = buildVersion,
        commitSha = commitSha,
        envId = envId
    )
}

fun run(
    scanPaths: List<String>,
    packagePrefixes: List<String>,
    verbose: Boolean = false,
    apiUrl: String,
    apiKey: String,
    groupId: String,
    appId: String,
    buildVersion: String?,
    commitSha: String?,
    envId: String?
) {
    val methods = mutableListOf<AstMethod>()
    var classCount = ClassLoadersScanner(
        packagePrefixes,
        50,
        scanPaths,
        false
    ) { classes ->
        if (verbose) {
            classes.forEach { println("Scanning ${it.entityName()}") }
        }
        methods += classes.flatMap { parseAstClass(it.entityName(), it.bytes()) }
    }.scanClasses()

    println("Scan complete. Scanned $classCount classes; ${methods.count()} methods")

    val instanceId = UUID.randomUUID().toString()

    val message = ClassMetadata(
        groupId = groupId,
        appId = appId,
        commitSha = commitSha,
        buildVersion = buildVersion,
        instanceId = instanceId,
        methods = methods
    )

    val agentMetadata = AgentMetadata(
        groupId = groupId,
        appId = appId,
        buildVersion = buildVersion,
        commitSha = commitSha,
        envId = envId,
        instanceId = instanceId,
        packagesPrefixes = packagePrefixes
    )

    val transport = HttpAgentMessageTransport(apiUrl, apiKey)

    // TODO support passing sslTruststore & sslTruststorePass
    transport.send(
        AgentMessageDestination("PUT", "/api/data-ingest/instances"),
        Json.encodeToString(AgentMetadata.serializer(), agentMetadata).toByteArray(),
        "application/json"
    )

    transport.send(
        AgentMessageDestination("PUT", "/api/data-ingest/methods"),
        Json.encodeToString(ClassMetadata.serializer(), message).toByteArray(),
        "application/json"
    )
    println("done!")
}
