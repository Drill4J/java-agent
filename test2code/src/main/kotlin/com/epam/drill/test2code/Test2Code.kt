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

import com.epam.drill.common.agent.*
import com.epam.drill.common.classloading.*
import com.epam.drill.plugins.test2code.common.api.*
import com.epam.drill.test2code.classloading.*
import com.epam.drill.test2code.classparsing.*
import com.epam.drill.test2code.coverage.*
import kotlinx.serialization.json.*
import mu.*
import java.util.concurrent.*

const val DRIlL_TEST_NAME_HEADER = "drill-test-name"
const val DRILL_TEST_ID_HEADER = "drill-test-id"

/**
 * Service for managing the plugin on the agent side
 */
@Suppress("unused")
class Test2Code(
    id: String,
    agentContext: AgentContext,
    sender: Sender
) : AgentModule<AgentAction>(id, agentContext, sender), Instrumenter, ClassScanner {

    internal val logger = KotlinLogging.logger {}

    internal val json = Json { encodeDefaults = true }

    private val coverageManager = CoverageManager(coverageTransport = WebsocketCoverageTransport(id, sender))

    private val instrumenter = DrillInstrumenter(coverageManager, coverageManager)

    //TODO remove after admin refactoring
    private val sessions = ConcurrentHashMap<String, Boolean>()

    override fun onConnect() {}

    /**
     * Switch on the plugin
     * @features Agent registration
     */
    override fun on() {
        val initInfo = InitInfo(message = "Initializing plugin $id...")
        sendMessage(initInfo)
        logger.info { "Initializing plugin $id..." }

        scanAndSendMetadataClasses()
        sendMessage(Initialized(msg = "Initialized"))
        logger.info { "Plugin $id initialized!" }
    }

    override fun instrument(
        className: String,
        initialBytes: ByteArray,
    ): ByteArray? = instrumenter.instrument(className, initialBytes)

    override fun load() {
        logger.info { "Plugin $id: initializing..." }
        coverageManager.startSendingCoverage()
        logger.info { "Plugin $id initialized!" }
    }

    // TODO remove after merging to java-agent repo
    override fun doAction(action: AgentAction) {}


    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerRequest() {
        val sessionId = context() ?: GLOBAL_SESSION_ID
        val testName = context[DRIlL_TEST_NAME_HEADER] ?: DEFAULT_TEST_NAME
        val testId = context[DRILL_TEST_ID_HEADER] ?: testName.id()
        coverageManager.startRecording(sessionId, testId, testName)
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerResponse() {
        val sessionId = context() ?: GLOBAL_SESSION_ID
        val testName = context[DRIlL_TEST_NAME_HEADER] ?: DEFAULT_TEST_NAME
        val testId = context[DRILL_TEST_ID_HEADER] ?: testName.id()
        coverageManager.stopRecording(sessionId, testId, testName)
    }

    override fun parseAction(
        rawAction: String,
    ): AgentAction = json.decodeFromString(AgentAction.serializer(), rawAction)

    override fun scanClasses(consumer: (Set<EntitySource>) -> Unit) {
        NativeCalls.waitClassScanning()
        val packagePrefixes = NativeCalls.getPackagePrefixes().split(", ")
        val additionalPaths = NativeCalls.getScanClassPath().split(";")
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
                .let(::InitDataPart)
                .also(::sendMessage)
                .also { classCount += it.astEntities.size }
        }
        logger.info { "Scanned $classCount classes" }
    }


    private fun sendMessage(message: CoverMessage) {
        val messageStr = json.encodeToString(CoverMessage.serializer(), message)
        logger.debug { "Send message $messageStr" }
        send(messageStr)
    }

}