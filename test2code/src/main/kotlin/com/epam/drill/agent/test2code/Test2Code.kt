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
package com.epam.drill.agent.test2code

import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import com.epam.drill.agent.common.AgentContext
import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.module.AgentModule
import com.epam.drill.agent.common.module.Instrumenter
import com.epam.drill.agent.common.request.RequestProcessor
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import com.epam.drill.agent.common.classloading.EntitySource
import com.epam.drill.agent.configuration.AgentParametersValidator
import com.epam.drill.agent.test2code.common.api.AstMethod
import com.epam.drill.agent.test2code.common.transport.ClassMetadata
import com.epam.drill.agent.test2code.classloading.ClassLoadersScanner
import com.epam.drill.agent.test2code.classloading.ClassScanner
import com.epam.drill.agent.test2code.classparsing.parseAstClass
import com.epam.drill.agent.test2code.configuration.Test2CodeParameterDefinitions
import com.epam.drill.agent.test2code.configuration.Test2CodeParameterDefinitions.COVERAGE_COLLECTION_ENABLED
import com.epam.drill.agent.test2code.coverage.*

private const val DRILL_TEST_ID_HEADER = "drill-test-id"

/**
 * Service for managing the plugin on the agent side
 */
@Suppress("unused")
class Test2Code(
    id: String,
    agentContext: AgentContext,
    sender: AgentMessageSender,
    configuration: AgentConfiguration
) : AgentModule(id, agentContext, sender, configuration), Instrumenter, ClassScanner, RequestProcessor {

    internal val logger = KotlinLogging.logger {}
    internal val json = Json { encodeDefaults = true }

    private val coverageManager = DrillCoverageManager
    private val instrumenter = DrillInstrumenter(coverageManager)
    private val coverageSender: CoverageSender = IntervalCoverageSender(
        groupId = configuration.agentMetadata.groupId,
        appId = configuration.agentMetadata.appId,
        instanceId = configuration.agentMetadata.instanceId,
        intervalMs = configuration.parameters[Test2CodeParameterDefinitions.COVERAGE_SEND_INTERVAL],
        pageSize = configuration.parameters[Test2CodeParameterDefinitions.COVERAGE_SEND_PAGE_SIZE],
        sender = sender,
        collectReleasedProbes = { coverageManager.pollRecorded() },
        collectUnreleasedProbes = { coverageManager.getUnreleased() }
    )
    private val coverageCollectionEnabled = configuration.parameters[COVERAGE_COLLECTION_ENABLED]
    private val classScanningEnabled = configuration.parameters[Test2CodeParameterDefinitions.CLASS_SCANNING_ENABLED]

    override fun onConnect() {}

    override fun instrument(
        className: String,
        initialBytes: ByteArray,
    ): ByteArray? = takeIf { coverageCollectionEnabled }?.let {
        instrumenter.instrument(className, initialBytes)
    }

    override fun load() {
        AgentParametersValidator(configuration.parameters).validate(
            Test2CodeParameterDefinitions.SCAN_CLASS_PATH,
            Test2CodeParameterDefinitions.SCAN_CLASS_DELAY
        )
        thread {
            scanAndSendMetadataClasses()
        }
        if (coverageCollectionEnabled) {
            coverageSender.startSendingCoverage()
            Runtime.getRuntime().addShutdownHook(Thread { coverageSender.stopSendingCoverage() })
        } else {
            logger.info { "Coverage collection is disabled" }
        }
    }

    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    override fun processServerRequest() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null && testId == null) return
        coverageManager.startRecording(sessionId, testId)
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    override fun processServerResponse() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null && testId == null) return
        coverageManager.stopRecording(sessionId, testId)
    }

    override fun scanClasses(consumer: (Set<EntitySource>) -> Unit) {
        val packagePrefixes = configuration.agentMetadata.packagesPrefixes
        val scanClassPaths = configuration.parameters[Test2CodeParameterDefinitions.SCAN_CLASS_PATH] as List<String>
        val enableScanClassLoaders = configuration.parameters[Test2CodeParameterDefinitions.ENABLE_SCAN_CLASS_LOADERS]
        val scanClassDelay = configuration.parameters[Test2CodeParameterDefinitions.SCAN_CLASS_DELAY]
        if (enableScanClassLoaders && scanClassDelay.isPositive()) {
            logger.debug { "Waiting class scan delay ${scanClassDelay.inWholeMilliseconds} ms..." }
            runBlocking { delay(scanClassDelay) }
        }
        logger.info { "Scanning classes, package prefixes: $packagePrefixes... " }
        ClassLoadersScanner(
            packagePrefixes,
            50,
            scanClassPaths,
            enableScanClassLoaders,
            consumer
        ).scanClasses()
    }

    /**
     * Scan, parse and send metadata classes to the admin side
     */
    fun scanAndSendMetadataClasses() {
        if (!classScanningEnabled) {
            logger.info { "Class scanning is disabled" }
            return
        }
        var classCount = 0
        var methodCount = 0
        scanClasses { classes ->
            classes
                .also { classCount += it.size }
                .flatMap { parseAstClass(it.entityName(), it.bytes()) }
                .also { methodCount += it.size }
                .chunked(configuration.parameters[Test2CodeParameterDefinitions.METHODS_SEND_PAGE_SIZE])
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
        sender.send(classMetadataDestination, message, ClassMetadata.serializer())
    }

}
