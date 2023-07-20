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

import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import com.epam.drill.plugins.test2code.classloading.ClassLoadersScanner
import com.epam.drill.plugins.test2code.common.api.*
import com.epam.drill.common.classloading.EntitySource
import com.github.luben.zstd.*
import kotlinx.atomicfu.*
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.*
import java.util.*
import mu.KotlinLogging

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

    private val _enabled = atomic(false)

    private val enabled: Boolean get() = _enabled.value

    private val instrContext: SessionProbeArrayProvider = DrillProbeArrayProvider.apply {
        defaultContext = agentContext
    }

    private val instrumenter = DrillInstrumenter(instrContext)

    private val _retransformed = atomic(false)

    override fun onConnect() {
        val ids = instrContext.getActiveSessions()
        logger.info { "Send active sessions after reconnect: ${ids.count()}" }
        sendMessage(SyncMessage(ids))
    }

    //TODO remove
    override fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }

    //TODO remove
    override fun isEnabled(): Boolean = _enabled.value

    /**
     * Switch on the plugin
     * @features Agent registration
     */
    override fun on() {
        val initInfo = InitInfo(message = "Initializing plugin $id...")
        sendMessage(initInfo)
        logger.info { "Initializing plugin $id..." }

        scanAndSendMetadataClasses()

        if (_retransformed.compareAndSet(expect = false, update = true)) {
            retransform()
        }
        sendMessage(Initialized(msg = "Initialized"))
        logger.info { "Plugin $id initialized!" }
    }

    /**
     * Switch off the plugin
     */
    override fun off() {
        logger.info { "Enabled $enabled" }
        val cancelledCount = instrContext.cancelAll()
        logger.info { "Plugin $id is off" }
        if (_retransformed.compareAndSet(expect = true, update = false)) {
            retransform()
        }
        sendMessage(SessionsCancelled(cancelledCount, currentTimeMillis()))
    }

    /**
     * Retransforming does not require an agent part instance.
     * This method is used in integration tests.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun retransform() {
        try {
            Native.RetransformClassesByPackagePrefixes(byteArrayOf())
        } catch (ex: Throwable) {
            logger.error(ex) { "Error retransforming classes." }
        }
    }

    override fun instrument(
        className: String,
        initialBytes: ByteArray,
    ): ByteArray? = takeIf { enabled }?.run {
        instrumenter.instrument(className, initialBytes)
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {}

    override fun initPlugin() {
        logger.info { "Plugin $id: initializing..." }
        retransform()
        _retransformed.value = true
    }

    override suspend fun doAction(action: AgentAction) {
        when (action) {
            /**
             * @features Session starting
             */
            is StartAgentSession -> action.payload.run {
                logger.info { "Start recording for session $sessionId (isGlobal=$isGlobal)" }
                val handler = probeSender(sessionId, isRealtime)
                instrContext.start(sessionId, isGlobal, testName, handler)
                sendMessage(SessionStarted(sessionId, testType, isRealtime, currentTimeMillis()))
            }
            is AddAgentSessionData -> {
                //ignored
            }
            is AddAgentSessionTests -> action.payload.run {
                instrContext.addCompletedTests(sessionId, tests)
            }
            /**
             * @features Session stopping
             */
            is StopAgentSession -> {
                val sessionId = action.payload.sessionId
                logger.info { "End of recording for session $sessionId" }
                val runtimeData = instrContext.stop(sessionId) ?: emptySequence()
                if (runtimeData.any()) {
                    probeSender(sessionId)(runtimeData)
                } else logger.info { "No data for session $sessionId" }
                sendMessage(SessionFinished(sessionId, currentTimeMillis()))
            }
            is StopAllAgentSessions -> {
                val stopped = instrContext.stopAll()
                logger.info { "End of recording for sessions $stopped" }
                for ((sessionId, data) in stopped) {
                    if (data.any()) {
                        probeSender(sessionId)(data)
                    }
                }
                val ids = stopped.map { it.first }
                sendMessage(SessionsFinished(ids, currentTimeMillis()))
            }
            is CancelAgentSession -> {
                val sessionId = action.payload.sessionId
                logger.info { "Cancellation of recording for session $sessionId" }
                instrContext.cancel(sessionId)
                sendMessage(SessionCancelled(sessionId, currentTimeMillis()))
            }
            is CancelAllAgentSessions -> {
                val cancelled = instrContext.cancelAll()
                logger.info { "Cancellation of recording for sessions $cancelled" }
                sendMessage(SessionsCancelled(cancelled, currentTimeMillis()))
            }
            else -> Unit
        }
    }

    /**
     * When the application under test receive a request from the caller
     * For each request we fill the thread local variable with an array of [ExecDatum]
     * @features Running tests
     */
    fun processServerRequest() {
        (instrContext as DrillProbeArrayProvider).run {
            val sessionId = context()
            val name = context[DRIlL_TEST_NAME_HEADER] ?: DEFAULT_TEST_NAME
            val id = context[DRILL_TEST_ID_HEADER] ?: name.id()
            val testKey = TestKey(name, id)
            runtimes[sessionId]?.run {
                val execDatum = getOrPut(testKey) {
                    arrayOfNulls<ExecDatum>(MAX_CLASS_COUNT).apply { fillFromMeta(testKey) }
                }
                logger.trace { "processServerRequest. thread '${Thread.currentThread().id}' sessionId '$sessionId' testKey '$testKey'" }
                requestThreadLocal.set(execDatum)
            }
        }
    }

    /**
     * When the application under test returns a response to the caller
     * @features Running tests
     */
    fun processServerResponse() {
        (instrContext as DrillProbeArrayProvider).run {
            requestThreadLocal.remove()
        }
    }

    override fun parseAction(
        rawAction: String,
    ): AgentAction = json.decodeFromString(AgentAction.serializer(), rawAction)

    override fun scanClasses(consumer: (Set<EntitySource>) -> Unit) {
        Native.WaitClassScanning()
        val packagePrefixes = Native.GetPackagePrefixes().split(", ")
        val additionalPaths = Native.GetScanClassPath().split(";")
        logger.info { "Scanning classes, package prefixes: $packagePrefixes... " }
        ClassLoadersScanner(packagePrefixes, 50, consumer).scanClasses(additionalPaths)
    }

    /**
     * Scan, parse and send metadata classes to the admin side
     */
    private fun scanAndSendMetadataClasses() {
        var classCount = 0;
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
