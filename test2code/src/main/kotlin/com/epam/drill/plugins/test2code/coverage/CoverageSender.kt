package com.epam.drill.plugins.test2code.coverage

interface CoverageSender {
    fun setSendingHandler(handler: RealtimeHandler)
    fun startSendingCoverage()
    fun stopSendingCoverage()
}