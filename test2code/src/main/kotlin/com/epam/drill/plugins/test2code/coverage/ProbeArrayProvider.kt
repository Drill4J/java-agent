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
package com.epam.drill.plugins.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import com.epam.drill.jacoco.StubAgentProbes
import com.epam.drill.plugins.test2code.common.api.DEFAULT_TEST_NAME
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbeArrayProvider = (ClassId, Int, String, Int) -> AgentProbes


/**
 * Simple probe array provider that employs a lock-free map for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class SimpleSessionProbeArrayProvider() : ProbeArrayProvider, ProbeDescriptorProvider,
    CoverageRecorder, CoverageSender {

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

    private var globalExecData = ExecData()
    private val stubProbes = StubAgentProbes()
    private val probeMetaContainer = ConcurrentHashMap<ClassId, ProbeDescriptor>()
    private val execDataPool: DataPool<SessionTestKey, ExecData> = ConcurrentDataPool()

    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = getSessionClassProbes(id)
        ?: getGlobalClassProbes(id)
        ?: stubProbes

    override fun addProbeDescriptor(descriptor: ProbeDescriptor) {
        probeMetaContainer[descriptor.id] = descriptor
        globalExecData.getOrPut(descriptor.id) {
            descriptor.toExecDatum()
        }
    }

    override fun ExecData.fillExecData(
        sessionId: String,
        testId: String
    ) {
        probeMetaContainer.values.forEach { descriptor ->
            this.putIfAbsent(
                descriptor.id, ExecDatum(
                    id = descriptor.id,
                    name = descriptor.name,
                    probes = AgentProbes(descriptor.probeCount),
                    sessionId = sessionId,
                    testId = testId
                )
            )
        }
    }

    override fun startRecording(sessionId: String, testId: String) {
        val data = execDataPool.getOrPut(SessionTestKey(sessionId, testId )) {
            ExecData().apply { fillExecData(sessionId, testId) }
        }
        requestThreadLocal.set(data)
        logger.trace { "Test recording started (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun stopRecording(sessionId: String, testId: String) {
        val data = requestThreadLocal.get()
        if (data != null) {
            execDataPool.release(SessionTestKey(sessionId, testId), data)
            requestThreadLocal.remove()
        }
        logger.trace { "Test recording stopped (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun collectProbes(): Sequence<ExecDatum> {
        releaseGlobalExecData()
        return execDataPool.pollReleased().flatMap { it.values }.filter { it.probes.hasPositive() }
    }

    private fun releaseGlobalExecData() {
        globalExecData.values.filter { datum ->
            datum.probes.hasPositive()
        }.map { datum ->
            val probesToSend = datum.probes.values.copyOf()
            probesToSend.forEachIndexed { index, value ->
                if (value)
                    datum.probes.values[index] = false
            }
            datum.copy(
                probes = AgentProbes(
                    values = probesToSend
                )
            )
        }.associateByTo(ExecData()) { it.id }.also { data ->
            execDataPool.release(GLOBAL_SESSION_TEST_KEY, data)
        }
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
    private fun getSessionClassProbes(id: ClassId) = requestThreadLocal.get()?.get(id)?.probes

    private fun getGlobalClassProbes(id: ClassId) = globalExecData[id]?.probes

    private fun ProbeDescriptor.toExecDatum(
        sessionId: String = GLOBAL_SESSION_ID,
        testId: String = DEFAULT_TEST_ID,
    ) = ExecDatum(
        id = id,
        name = name,
        probes = AgentProbes(probeCount),
        sessionId = sessionId,
        testId = testId
    )


}

internal object ProbeWorker : CoroutineScope {
    override val coroutineContext: CoroutineContext = run {
        // TODO ProbeWorker thread count configure via env.variable?
        Executors.newFixedThreadPool(2).asCoroutineDispatcher() + SupervisorJob()
    }
}

/**
 * The probe provider MUST be a Kotlin singleton object
 */
internal object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider()
