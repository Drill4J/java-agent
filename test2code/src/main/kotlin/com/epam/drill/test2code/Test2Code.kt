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
package com.epam.drill.test2code

import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import com.epam.drill.common.agent.AgentContext
import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.module.AgentModule
import com.epam.drill.common.agent.module.Instrumenter
import com.epam.drill.common.agent.request.RequestProcessor
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.classloading.EntitySource
import com.epam.drill.plugins.test2code.common.api.AstMethod
import com.epam.drill.plugins.test2code.common.transport.ClassMetadata
import com.epam.drill.test2code.classloading.ClassLoadersScanner
import com.epam.drill.test2code.classloading.ClassScanner
import com.epam.drill.test2code.classparsing.parseAstClass
import com.epam.drill.test2code.configuration.ParameterDefinitions
import com.epam.drill.test2code.configuration.ParametersValidator
import com.epam.drill.test2code.coverage.*

private const val DRILL_TEST_ID_HEADER = "drill-test-id"

/**
 * Service for managing the plugin on the agent side
 */
@Suppress("unused")
class Test2Code(
    id: String,
    agentContext: AgentContext,
    sender: AgentMessageSender<AgentMessage>,
    configuration: AgentConfiguration
) : AgentModule(id, agentContext, sender, configuration), Instrumenter, ClassScanner, RequestProcessor {

    internal val logger = KotlinLogging.logger {}
    internal val json = Json { encodeDefaults = true }

    private val coverageManager = DrillCoverageManager
    private val instrumenter = DrillInstrumenter(coverageManager, coverageManager)
    private val coverageSender: CoverageSender = IntervalCoverageSender(
        instanceId = configuration.agentMetadata.instanceId,
        intervalMs = configuration.parameters[ParameterDefinitions.COVERAGE_SEND_INTERVAL],
        pageSize = configuration.parameters[ParameterDefinitions.COVERAGE_SEND_PAGE_SIZE],
        sender = sender,
        collectProbes = { coverageManager.pollRecorded() }
    )

    override fun onConnect() {}

    override fun instrument(
        className: String,
        initialBytes: ByteArray,
    ): ByteArray? = instrumenter.instrument(className, initialBytes)

    override fun load() {
        ParametersValidator.validate(configuration.parameters)
        logger.debug { "load: Waiting for transport availability for class metadata scanning" }
        thread {
            scanAndSendMetadataClasses()
        }
        coverageSender.startSendingCoverage()
        Runtime.getRuntime().addShutdownHook(Thread { coverageSender.stopSendingCoverage() })
    }

    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    override fun processServerRequest() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null || testId == null) return
        coverageManager.startRecording(sessionId, testId)
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    override fun processServerResponse() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null || testId == null) return
        coverageManager.stopRecording(sessionId, testId)
    }

    override fun scanClasses(consumer: (Set<EntitySource>) -> Unit) {
        val packagePrefixes = configuration.agentMetadata.packagesPrefixes
        val scanClassPaths = configuration.parameters[ParameterDefinitions.SCAN_CLASS_PATH]
        val isScanClassLoaders = configuration.parameters[ParameterDefinitions.IS_SCAN_CLASS_LOADERS]
        val scanClassDelay = configuration.parameters[ParameterDefinitions.SCAN_CLASS_DELAY]
        if (isScanClassLoaders && scanClassDelay.isPositive()) {
            logger.debug { "Waiting class scan delay ${scanClassDelay.inWholeMilliseconds} ms..." }
            runBlocking { delay(scanClassDelay) }
        }
        logger.info { "Scanning classes, package prefixes: $packagePrefixes... " }
        ClassLoadersScanner(
            packagePrefixes,
            50,
            scanClassPaths,
            isScanClassLoaders,
            consumer
        ).scanClasses()
    }

    /**
     * Scan, parse and send metadata classes to the admin side
     */
    private fun scanAndSendMetadataClasses() {
        var classCount = 0
        var methodCount = 0
        scanClasses { classes ->
            classes
                .also { classCount += it.size }
                .flatMap { parseAstClass(it.entityName(), it.bytes()) }
                .also { methodCount += it.size }
                .chunked(configuration.parameters[ParameterDefinitions.METHODS_SEND_PAGE_SIZE])
                .forEach(::sendClassMetadata)
        }
        logger.info { "Scanned $classCount classes with $methodCount methods" }
    }

    private val classMetadataDestination = AgentMessageDestination("PUT", "methods")

    private fun sendClassMetadata(methods: List<AstMethod>) {
        val message = ClassMetadata(
            groupId = configuration.agentMetadata.groupId,
            appId = configuration.agentMetadata.appId,
            commitSha = configuration.agentMetadata.commitSha,
            buildVersion = configuration.agentMetadata.buildVersion,
            instanceId = configuration.agentMetadata.instanceId,
            methods = methods
        )
        logger.debug { "sendClassMetadata: Sending methods: $message" }
        sender.send(classMetadataDestination, message)
    }

}
