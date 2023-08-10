package com.epam.drill.plugins.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import com.epam.drill.plugins.test2code.common.api.DEFAULT_TEST_NAME

/**
 * Simple probe provider that employs a lock-free map for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class CoverageManager(
    // TODO EPMDJ-8256 When application is async we must use this implementation «com.alibaba.ttl.TransmittableThreadLocal»
    private val requestThreadLocal: ThreadLocal<ExecData?> = ThreadLocal(),
    private val globalExecData: ExecData = ExecData(),
    private val execDataPool: DataPool<SessionTestKey, ExecData> = ConcurrentDataPool(),

    private val probesProvider: ProbesProvider = SimpleProbesProvider(requestThreadLocal, globalExecData),
    private val probesDescriptorProvider: ProbesDescriptorProvider = ConcurrentProbesDescriptorProvider(),
    private val coverageRecorder: CoverageRecorder = ThreadCoverageRecorder(
        execDataPool,
        requestThreadLocal,
        probesDescriptorProvider
    ),
    private val coverageSender: CoverageSender = IntervalCoverageSender(coverageRecorder, 2000L)
) : ProbesProvider by probesProvider,
    ProbesDescriptorProvider by probesDescriptorProvider,
    CoverageRecorder by coverageRecorder,
    CoverageSender by coverageSender {

    override fun addDescriptor(descriptor: ProbesDescriptor) {
        probesDescriptorProvider.addDescriptor(descriptor)
        globalExecData.getOrPut(descriptor.id) {
            descriptor.toExecDatum()
        }
    }

    override fun collectProbes(): Sequence<ExecDatum> {
        releaseGlobalExecData()
        return coverageRecorder.collectProbes()
    }

    private fun ProbesDescriptor.toExecDatum(
        sessionId: String = GLOBAL_SESSION_ID,
        testId: String = DEFAULT_TEST_ID,
        testName: String = DEFAULT_TEST_NAME
    ) = ExecDatum(
        id = id,
        name = name,
        probes = AgentProbes(probeCount),
        sessionId = sessionId,
        testName = testName,
        testId = testId
    )


    private fun releaseGlobalExecData() {
        globalExecData.values.filter { datum ->
            datum.probes.containCovered()
        }.map { datum ->
            datum.copy(
                probes = AgentProbes(
                    values = datum.probes.values.copyOf()
                )
            ).also {
                datum.probes.values.fill(false)
            }
        }.associateByTo(ExecData()) { it.id }.also { data ->
            execDataPool.release(GLOBAL_SESSION_TEST_KEY, data)
        }
    }


}

/**
 * The probe provider MUST be a Kotlin singleton object
 */
internal object DrillProbesArrayProvider : CoverageManager()
