package com.epam.drill.agent.instrument.reactor

import com.epam.drill.common.agent.request.DrillRequest

class PropagatedDrillContextRunnable(private val drillRequest: DrillRequest, private val decorate: Runnable): Runnable {
    override fun run() {
        propagateDrillRequest(drillRequest) {
            decorate.run()
        }
    }
}