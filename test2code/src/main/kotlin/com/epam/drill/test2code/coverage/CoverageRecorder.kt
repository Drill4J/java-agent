package com.epam.drill.test2code.coverage


interface ICoverageRecorder {
    fun startRecording(sessionId: String, testId: String)
    fun stopRecording(sessionId: String, testId: String)
    fun getContext(): ContextCoverage?
    fun pollRecorded(): Sequence<ExecDatum>
}

data class ContextCoverage(val context: ContextKey, val execData: ExecData)