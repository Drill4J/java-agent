package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes

class GlobalCoverageRecorder: ICoverageRecorder {
    private val globalExecData: ExecData = ExecData()
    private val sentGlobalExecData: ExecData = ExecData()

    override fun startRecording(sessionId: String, testId: String) {
        // do nothing
    }

    override fun stopRecording(sessionId: String, testId: String) {
        // do nothing
    }

    override fun getContext(): ContextCoverage {
        return ContextCoverage(CONTEXT_AMBIENT, globalExecData)
    }

    override fun pollRecorded(): Sequence<ExecDatum> {
        var hasChanges = false
        globalExecData.forEach { (key, value) ->
            sentGlobalExecData.compute(key) { _, oldValue ->
                if (oldValue?.equals(value) != true) {
                    hasChanges = true
                    value.copy(probes = AgentProbes(values = value.probes.values.copyOf()))
                } else
                    oldValue
            }
        }
        return if (hasChanges) {
            sentGlobalExecData.values.asSequence().filter { it.probes.containCovered() }
        } else
            emptySequence()
    }
}
