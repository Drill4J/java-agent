package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.DrillRequest
import com.epam.drill.agent.request.RequestHolder
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PropagatedDrillContextRunnable(private val drillRequest: DrillRequest, val decorate: Runnable): Runnable {
    override fun run() {
        try {
            logger.info { "[${Thread.currentThread().name}] task ran: ${drillRequest}" }
            RequestHolder.storeRequest(drillRequest)
            decorate.run()
        } finally {
            RequestHolder.remove()
            logger.info { "[${Thread.currentThread().name}] task finished: ${drillRequest}" }
        }
    }
}