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
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging
import com.epam.drill.common.agent.AgentContext
import com.epam.drill.common.agent.AgentModule
import com.epam.drill.common.agent.Instrumenter
import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.classloading.EntitySource
import com.epam.drill.plugins.test2code.common.api.AstEntity
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
    sender: AgentMessageSender,
    configuration: AgentConfiguration
) : AgentModule(id, agentContext, sender, configuration), Instrumenter, ClassScanner {

    internal val logger = KotlinLogging.logger {}
    internal val json = Json { encodeDefaults = true }

    private val coverageManager = DrillCoverageManager.apply {
        setCoverageSendInterval(configuration.parameters[ParameterDefinitions.COVERAGE_SEND_INTERVAL])
        setAgentMessageSender(sender)
    }
    private val instrumenter = DrillInstrumenter(coverageManager, coverageManager)
    //TODO remove after admin refactoring
    private val sessions = ConcurrentHashMap<String, Boolean>()

    override fun onConnect() {}

    override fun instrument(
        className: String,
        initialBytes: ByteArray,
    ): ByteArray? = instrumenter.instrument(className, initialBytes)

    override fun load() {
        ParametersValidator.validate(configuration.parameters)
        logger.info { "load: Waiting for transport availability for class metadata scanning" }
        thread {
            while(!sender.available) Thread.sleep(500)
            scanAndSendMetadataClasses()
            coverageManager.startSendingCoverage()
        }
    }

    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerRequest() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null || testId == null) return
        coverageManager.startRecording(sessionId, testId)
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerResponse() {
        val sessionId = context()
        val testId = context[DRILL_TEST_ID_HEADER]
        if (sessionId == null || testId == null) return
        coverageManager.stopRecording(sessionId, testId)
    }

    override fun scanClasses(consumer: (Set<EntitySource>) -> Unit) {
        val packagePrefixes = configuration.agentMetadata.packagesPrefixes
        val additionalPaths = configuration.parameters[ParameterDefinitions.SCAN_CLASS_PATH]
        val classScanDelay = configuration.parameters[ParameterDefinitions.CLASS_SCAN_DELAY]
        if (classScanDelay.isPositive()) {
            logger.debug { "Waiting class scan delay ${classScanDelay.inWholeMilliseconds} ms..." }
            runBlocking { delay(classScanDelay) }
        }
        logger.info { "Scanning classes, package prefixes: $packagePrefixes... " }
        ClassLoadersScanner(packagePrefixes, 50, consumer, additionalPaths).scanClasses()
    }

    /**
     * Scan, parse and send metadata classes to the admin side
     */
    private fun scanAndSendMetadataClasses() {
        var classCount = 0
        scanClasses { classes ->
            classes
                .map { parseAstClass(it.entityName(), it.bytes()) }
                .also(::sendClassMetadata)
                .also { classCount += it.size }
        }
        sendClassMetadataComplete()
        logger.info { "Scanned $classCount classes" }
    }

    private val classMetadataDestination = AgentMessageDestination("POST", "class-metadata")
    private val classMetadataCompleteDestination = AgentMessageDestination("POST", "class-metadata/complete")

    private fun sendClassMetadata(astEntities: List<AstEntity>) {
        val message = ClassMetadata(astEntities = astEntities)
        logger.debug { "sendClassMetadata: Sending class metadata: $message" }
        sender.send(classMetadataDestination, message)
    }

    private fun sendClassMetadataComplete() {
        logger.debug { "sendClassMetadataComplete: Sending class metadata complete message" }
        sender.send(classMetadataCompleteDestination, AgentMessage())
    }

}
