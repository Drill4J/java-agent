package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.DrillRequest
import com.epam.drill.agent.request.RequestHolder
import java.util.function.Supplier

object DrillSupplier: Supplier<Any?> {
    override fun get(): DrillRequest? {
        return RequestHolder.getRequest()
    }
}