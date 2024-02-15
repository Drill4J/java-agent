package com.epam.drill.agent.instrument.reactor

import com.epam.drill.common.agent.request.DrillRequest
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PropagatedDrillContextRunnable(private val drillRequest: DrillRequest, private val decorate: Runnable): Runnable {
    override fun run() {
        propagateDrillRequest(drillRequest) {
            logger.trace { "run task, sessionId = ${drillRequest.drillSessionId}" }
            decorate.run()
        }
    }
}