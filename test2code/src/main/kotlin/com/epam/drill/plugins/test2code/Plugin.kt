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
import com.epam.drill.plugins.test2code.DrillProbeArrayProvider.fillFromMeta
import com.epam.drill.plugins.test2code.classloading.ClassLoadersScanner
import com.epam.drill.plugins.test2code.common.api.*
import com.github.luben.zstd.Zstd
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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

    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider.apply {
        defaultContext = agentContext
    }

    private val instrumenter = DrillInstrumenter(instrContext)

    override fun onConnect() {
        val ids = instrContext.getActiveSessions()
        logger.info { "Send active sessions after reconnect: ${ids.count()}" }
        sendMessage(SyncMessage(ids))
    }

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
//        //Create global session
        val sessionId = "global"
        val isRealtime = true
        val handler = probeSender(sessionId, isRealtime)
        val isGlobal = true
        instrContext.start(sessionId, isGlobal, "GlobalSession", handler)
        //TODO add creation agent-session here and remove checking on active-session on admin part
        sendMessage(SessionStarted(sessionId, "AUTO", isRealtime, isGlobal, currentTimeMillis()))
        logger.info { "Plugin $id: global session was created." }
    }

    // TODO remove after merging to java-agent repo
    override fun doAction(action: AgentAction) {
        when (action) {
            /**
             * @features Session starting
             */
            is StartAgentSession -> action.payload.run {
//                logger.info { "Start recording for session $sessionId (isGlobal=$isGlobal)" }
//                val handler = probeSender(sessionId, isRealtime)
//                instrContext.start(sessionId, isGlobal, testName, handler)
//                sendMessage(SessionStarted(sessionId, testType, isRealtime, currentTimeMillis()))
            }

            is AddAgentSessionData -> {
                //ignored
            }

            is AddAgentSessionTests -> action.payload.run {
//                instrContext.addCompletedTests(sessionId, tests)
            }
            /**
             * @features Session stopping
             */
            is StopAgentSession -> {
//                val sessionId = action.payload.sessionId
//                logger.info { "End of recording for session $sessionId" }
//                val runtimeData = instrContext.stop(sessionId) ?: emptySequence()
//                if (runtimeData.any()) {
//                    probeSender(sessionId)(runtimeData)
//                } else logger.info { "No data for session $sessionId" }
//                sendMessage(SessionFinished(sessionId, currentTimeMillis()))
            }

            is StopAllAgentSessions -> {
//                val stopped = instrContext.stopAll()
//                logger.info { "End of recording for sessions $stopped" }
//                for ((sessionId, data) in stopped) {
//                    if (data.any()) {
//                        probeSender(sessionId)(data)
//                    }
//                }
//                val ids = stopped.map { it.first }
//                sendMessage(SessionsFinished(ids, currentTimeMillis()))
            }

            is CancelAgentSession -> {
//                val sessionId = action.payload.sessionId
//                logger.info { "Cancellation of recording for session $sessionId" }
//                instrContext.cancel(sessionId)
//                sendMessage(SessionCancelled(sessionId, currentTimeMillis()))
            }

            is CancelAllAgentSessions -> {
//                val cancelled = instrContext.cancelAll()
//                logger.info { "Cancellation of recording for sessions $cancelled" }
//                sendMessage(SessionsCancelled(cancelled, currentTimeMillis()))
            }

            else -> Unit
        }
    }

    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerRequest() {
        logger.trace { "processServerRequest before instrContext. thread '${Thread.currentThread().id}' " }
        val drillProbeArrayProvider = instrContext as DrillProbeArrayProvider

        // TODO session id can be null
        val sessionId = context() ?: return

        val name = context[DRIlL_TEST_NAME_HEADER] ?: DEFAULT_TEST_NAME
        val id = context[DRILL_TEST_ID_HEADER] ?: name.id()
        val testKey = TestKey(name, id)

        // Start runtime + agent session if none created for supplied context.sessionId.
        if (drillProbeArrayProvider.runtimes[sessionId] == null) {
            logger.trace { "processServerRequest. session is null" }
            drillProbeArrayProvider.runtimes.forEach { logger.trace { "runtime: $it" } }
            val handler = probeSender(sessionId, true)
            val isGlobal = false
            instrContext.start(sessionId, isGlobal, name, handler)
            sendMessage(SessionStarted(sessionId, "AUTO", true, isGlobal, currentTimeMillis()))
        }
        val runtime = drillProbeArrayProvider.runtimes[sessionId]
        if (runtime == null) {
            logger.trace { "processServerRequest. thread '${Thread.currentThread().id}' sessionId '$sessionId' testKey '$testKey' runtime is null" }
            return
        }

        // Check on null
        if (sessionTestKeyPairToThreadNumber[Pair(sessionId, testKey)] == null) {
            // Create if it does not exist
            sessionTestKeyPairToThreadNumber[Pair(sessionId, testKey)] = AtomicInteger(0)
        }
        // Increment value for thread
        logger.trace { "CATDOG. processServerRequest. before incrementAndGet thread '${Thread.currentThread().id}' $sessionTestKeyPairToThreadNumber " }
        sessionTestKeyPairToThreadNumber[Pair(sessionId, testKey)]?.incrementAndGet()
        logger.trace { "CATDOG. processServerRequest. after incrementAndGet thread '${Thread.currentThread().id}' $sessionTestKeyPairToThreadNumber " }

        // TODO potential concurrency issue (if execData is removed by timer)
        val execData = runtime.getOrPut(Pair(sessionId, testKey)) {
            ExecData().apply { fillFromMeta(testKey) }
        }

        logger.trace { "CATDOG. processServerRequest. thread '${Thread.currentThread().id}' sessionId '$sessionId' testKey '$testKey'" }
        drillProbeArrayProvider.requestThreadLocal.set(execData)
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    @Suppress("UNUSED")
    fun processServerResponse() {
        (instrContext as DrillProbeArrayProvider).run {
            val sessionId = context()
            val name = context[DRIlL_TEST_NAME_HEADER] ?: DEFAULT_TEST_NAME
            val id = context[DRILL_TEST_ID_HEADER] ?: name.id()
            val testKey = TestKey(name, id)

            sessionTestKeyPairToThreadNumber[Pair(sessionId, testKey)]?.decrementAndGet()
            logger?.trace { "CATDOG. processServerResponse. after decrementAtomicInt thread '${Thread.currentThread().id}' $sessionTestKeyPairToThreadNumber " }
            requestThreadLocal.remove()
        }
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

val sessionTestKeyPairToThreadNumber = ConcurrentHashMap<Pair<String, TestKey>, AtomicInteger>()

//TODO impl
val sessionToThreadNumber = ConcurrentHashMap<String, AtomicInteger>()

/**
 * Create a function which sends chunks of test coverage to the admin part of the plugin
 * @param sessionId the test session ID
 * @param sendChanged the sign of the need to send the number of data
 * @return the function of sending test coverage
 * @features Coverage data sending
 */
fun Plugin.probeSender(
    sessionId: String,
    sendChanged: Boolean = false,
): RealtimeHandler = { execData ->
    execData
        .map(ExecDatum::toExecClassData)
        .chunked(0xffff)
        .map { chunk -> CoverDataPart(sessionId, chunk) }
        .sumOf { message ->
            logger.trace { "send to admin-part '$message'..." }
            val encoded = ProtoBuf.encodeToByteArray(CoverMessage.serializer(), message)
            val compressed = Zstd.compress(encoded)
            send(Base64.getEncoder().encodeToString(compressed))
            message.data.count()
        }.takeIf { sendChanged && it > 0 }?.let {
            sendMessage(SessionChanged(sessionId, it))
        }
}

fun Plugin.sendMessage(message: CoverMessage) {
    val messageStr = json.encodeToString(CoverMessage.serializer(), message)
    logger.debug { "Send message $messageStr" }
    send(messageStr)
}
