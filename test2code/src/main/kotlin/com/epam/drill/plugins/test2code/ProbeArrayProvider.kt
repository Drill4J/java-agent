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

import com.epam.drill.jacoco.AgentProbes
import com.epam.drill.jacoco.StubAgentProbes
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (Long, Int, String, Int) -> AgentProbes
typealias TestKey = Pair<String, String>
typealias SessionTestKey = Pair<String, TestKey>
interface SessionManager {

    fun startSession(
        sessionId: String,
        isGlobal: Boolean,
        testId: String,
        testName: String,
    )

    fun stopSession(sessionId: String)
}

class ProbeDescriptor(
    val id: Long,
    val name: String,
    val probeCount: Int,
)

interface ProbeDescriptorProvider {
    fun addProbeDescriptor(descriptor: ProbeDescriptor)
    fun ExecData.fillExecData(sessionId: String, testId: String, testName: String)
}

interface CoverageRecorder {
    fun startRecording(sessionId: String, testId: String, testName: String)
    fun stopRecording(sessionId: String, testId: String, testName: String)
    fun collectProbes(): Sequence<ExecDatum>
}

interface CoverageSender {
    fun setSendingHandler(handler: RealtimeHandler)
    fun startSendingCoverage()
    fun stopSendingCoverage()
}

internal object ProbeWorker : CoroutineScope {
    override val coroutineContext: CoroutineContext = run {
        // TODO ProbeWorker thread count configure via env.variable?
        Executors.newFixedThreadPool(2).asCoroutineDispatcher() + SupervisorJob()
    }
}

/**
 * Simple probe array provider that employs a lock-free map for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class SimpleSessionProbeArrayProvider() : ProbeArrayProvider, SessionManager, ProbeDescriptorProvider, CoverageRecorder, CoverageSender {

    private val logger = KotlinLogging.logger {}

    private var realtimeHandler: RealtimeHandler = StubRealtimeHandler()


    // TODO EPMDJ-8256 When application is async we must use this implementation «com.alibaba.ttl.TransmittableThreadLocal»
    private val requestThreadLocal = ThreadLocal<ExecData?>()

    private val job = ProbeWorker.launch(start = CoroutineStart.LAZY) {
        while (true) {
            delay(2000L)
            realtimeHandler(collectProbes())
        }
    }

    @Volatile
    private var global: Pair<String, ExecData>? = null
    private val stubProbes = StubAgentProbes()
    private val probeMetaContainer = ConcurrentHashMap<Long, ProbeDescriptor>()
    private val execData = ConcurrentDataPool<SessionTestKey, ExecData>()

    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = getSessionClassProbes(id)
        ?: getGlobalClassProbes(id)
        ?: stubProbes

    override fun startSession(
        sessionId: String,
        isGlobal: Boolean,
        testId: String,
        testName: String,
    ) {
        if (isGlobal) {
            global = sessionId to ExecData().apply { fillExecData(sessionId, testId, testName) }
        }
    }

    /**
     * Remove the test session from the active session list and return probes
     * @features Session finishing
     */
    override fun stopSession(sessionId: String) {
        if (isGlobal(sessionId)) {
            global = null
        }
    }

    override fun addProbeDescriptor(descriptor: ProbeDescriptor) {
        probeMetaContainer[descriptor.id] = descriptor
    }

    override fun ExecData.fillExecData(sessionId: String, testId: String, testName: String) {
        probeMetaContainer.values.forEach { descriptor ->
            this.putIfAbsent(descriptor.id, ExecDatum(
                id = descriptor.id,
                name = descriptor.name,
                probes = AgentProbes(descriptor.probeCount),
                sessionId = sessionId,
                testName = testName,
                testId = testId
            ))
        }
    }

    override fun startRecording(sessionId: String, testId: String, testName: String) {
        val data = execData.getOrPut(SessionTestKey(sessionId, testId to testName)) {
            ExecData().apply { fillExecData(sessionId, testId, testName) }
        }
        requestThreadLocal.set(data)
        logger.trace { "Test recording started (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun stopRecording(sessionId: String, testId: String, testName: String) {
        val data = requestThreadLocal.get()
        if (data != null) {
            execData.release(SessionTestKey(sessionId, testId to testName), data)
            requestThreadLocal.remove()
            logger.trace { "Test recording stopped (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
        }
    }

    override fun collectProbes(): Sequence<ExecDatum> {
        return execData.pollReleased().flatMap { it.values }.filter { it.probes.hasProbes() }
    }

    override fun setSendingHandler(handler: RealtimeHandler) {
        this.realtimeHandler = handler
    }

    override fun startSendingCoverage() {
        job.start()
        logger.debug { "Coverage sending job is started." }
    }

    override fun stopSendingCoverage() {
        job.cancel()
        logger.debug { "Coverage sending job is stopped." }
    }

    /**
     * requestThreadLocal stores probes of classes for a specific session
     * (see requestThreadLocal.set(execData) in processServerRequest method in Plugin.kt)
     */
    private fun getSessionClassProbes(id: Long) = requestThreadLocal.get()?.get(id)?.probes

    private fun getGlobalClassProbes(id: Long) = global?.second?.get(id)?.probes
    /**
     * Is the session global?
     */
    private fun isGlobal(sessionId: String) = sessionId == global?.first
}
/**
 * The probe provider MUST be a Kotlin singleton object
 */
internal object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider()
