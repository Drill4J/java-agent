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
package com.epam.drill.plugins.test2code

import com.epam.drill.agent.NativeCalls
import com.epam.drill.common.classloading.ClassScanner
import com.epam.drill.common.classloading.EntitySource
import com.epam.drill.plugin.api.processing.AgentContext
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.Instrumenter
import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.plugins.test2code.classloading.ClassLoadersScanner
import com.epam.drill.plugins.test2code.classparsing.parseAstClass
import com.epam.drill.plugins.test2code.common.api.*
import com.epam.drill.plugins.test2code.coverage.*
import com.epam.drill.plugins.test2code.coverage.DrillCoverageManager
import com.epam.drill.plugins.test2code.coverage.toExecClassData
import com.github.luben.zstd.Zstd
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

const val DRIlL_TEST_NAME_HEADER = "drill-test-name"
const val DRILL_TEST_ID_HEADER = "drill-test-id"

/**
 * Service for managing the plugin on the agent side
 */
@Suppress("unused")
class Plugin(
    id: String,
    agentContext: AgentContext,
    sender: Sender
) : AgentPart<AgentAction>(id, agentContext, sender), Instrumenter, ClassScanner {

    internal val logger = KotlinLogging.logger {}

    internal val json = Json { encodeDefaults = true }

    private val coverageManager = DrillCoverageManager.apply { setSendingHandler(probeSender(sendChanged = true)) }

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
}

/**
 * Create a function which sends chunks of test coverage to the admin part of the plugin
 * @param sessionId the test session ID
 * @param sendChanged the sign of the need to send the number of data
 * @return the function of sending test coverage
 * @features Coverage data sending
 */
fun Plugin.probeSender(
    sendChanged: Boolean = false,
): SendingHandler = { execData ->
    execData
        .groupBy { it.sessionId }
        .forEach { (sessionId, data) ->
            data.map(ExecDatum::toExecClassData)
                .chunked(0xffff)
                .map { chunk -> CoverDataPart(sessionId, chunk) }
                .sumOf { message ->
                    logger.debug { "Send compressed message $message" }
                    val encoded = ProtoBuf.encodeToByteArray(CoverMessage.serializer(), message)
                    val compressed = Zstd.compress(encoded)
                    send(Base64.getEncoder().encodeToString(compressed))
                    message.data.count()
                }.takeIf { sendChanged && it > 0 }?.let {
                    sendMessage(SessionChanged(sessionId, it))
                }
        }
}

fun Plugin.sendMessage(message: CoverMessage) {
    val messageStr = json.encodeToString(CoverMessage.serializer(), message)
    logger.debug { "Send message $messageStr" }
    send(messageStr)
}
