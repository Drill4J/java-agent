package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.DrillRequest
import mu.KotlinLogging
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.function.Function

private val logger = KotlinLogging.logger {}

object SubscriptionInterceptor {

    @RuntimeType
    fun intercept(
        @FieldValue(DRILL_DELEGATE_FIELD) target: Any,
        @FieldValue(DRILL_REQUEST_FIELD) drillRequest: DrillRequest,
        @Origin superMethod: Method,
        @Pipe pipe: Function<Any?, Any?>
    ): Any? {
        return when (superMethod.name) {
            "request", "cancel" -> requestOrCancel(target, drillRequest, superMethod, pipe)
            else -> pipe.apply(target)
        }
    }

    private fun requestOrCancel(
        target: Any,
        drillRequest: DrillRequest,
        superMethod: Method,
        pipe: Function<Any?, Any?>,
    ): Any? {
        return propagateDrillRequest(drillRequest) {
            logger.info("[" + Thread.currentThread().name + "] ${target.javaClass.simpleName}.${superMethod.name}:${target.hashCode()}, context $drillRequest")
            pipe.apply(target)
        }
    }
}