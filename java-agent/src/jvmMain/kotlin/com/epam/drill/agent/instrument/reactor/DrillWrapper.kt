package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.DrillRequest
import com.epam.drill.agent.request.RequestHolder
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

object DrillWrapper: BiFunction<Supplier<Any?>, Any, Any?> {
    override fun apply(s: Supplier<Any?>, ctx: Any): Any? {
        val previous = RequestHolder.getRequest()
        if (previous != null)
            RequestHolder.remove()
        try {
            RequestHolder.storeRequest(ctx as DrillRequest)
            return s.get()
        } finally {
            RequestHolder.remove()
            if (previous != null)
                RequestHolder.storeRequest(previous)
        }
    }
}