package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import java.util.concurrent.atomic.AtomicBoolean

class GlobalCoverageRecorder(enabled: Boolean = true): ICoverageRecorder {
    private val enabled = AtomicBoolean(enabled)
    private val globalExecData: ExecData = ExecData()
    private val sentGlobalExecData: ExecData = ExecData()

    override fun startRecording(sessionId: String, testId: String) {
        enabled.set(true)
    }

    override fun stopRecording(sessionId: String, testId: String) {
        enabled.set(false)
    }

    override fun getCurrent(): ContextCoverage? {
        return if (enabled.get()) ContextCoverage(CONTEXT_AMBIENT, globalExecData) else null
    }

    override fun pollRecorded(): Sequence<ExecDatum> {
        val newGlobalExecData = subtract(globalExecData, sentGlobalExecData)
        sentGlobalExecData.add(newGlobalExecData)
        return newGlobalExecData.values.asSequence()
            .filter { it.probes.containCovered() }
    }
}

internal fun AgentProbes.minus(subtrahend: AgentProbes): AgentProbes {
    val probes = AgentProbes(this.values.size)
    (values.indices).forEach { probes.values[it] = values[it] && !subtrahend.values[it] }
    return probes
}

internal fun AgentProbes.plus(addend: AgentProbes): AgentProbes {
    val probes = AgentProbes(this.values.size)
    (values.indices).forEach { probes.values[it] = values[it] || addend.values[it] }
    return probes
}

internal fun subtract(minuend: ExecData, subtrahend: ExecData): ExecData {
    val result = ExecData()
    for ((key, datum) in minuend) {
        val correspondingDatum = subtrahend[key]
        if (correspondingDatum == null) {
            val clonedProbes = datum.probes.values.copyOf()
            result[key] = datum.copy(probes = AgentProbes(clonedProbes.size, clonedProbes))
        } else if (!datum.probes.values.contentEquals(correspondingDatum.probes.values)) {
            result[key] = datum.copy(probes = datum.probes.minus(correspondingDatum.probes))
        }
    }
    return result
}

internal fun ExecData.add(addend: ExecData) {
    for ((key, datum) in addend) {
        val correspondingDatum = this[key]
        if (correspondingDatum == null) {
            this[key] = datum
        } else {
            this[key] = correspondingDatum.copy(probes = correspondingDatum.probes.plus(datum.probes))
        }
    }
}