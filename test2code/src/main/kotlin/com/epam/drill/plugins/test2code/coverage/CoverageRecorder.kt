package com.epam.drill.plugins.test2code.coverage

interface CoverageRecorder {
    fun startRecording(sessionId: String, testId: String, testName: String)
    fun stopRecording(sessionId: String, testId: String, testName: String)
    fun collectProbes(): Sequence<ExecDatum>
}