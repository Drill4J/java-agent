package com.epam.drill.agent.instrument.reactor

import com.epam.drill.common.agent.request.DrillRequest
import mu.KotlinLogging
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.function.Function

private val logger = KotlinLogging.logger {}

object SubscriberInterceptor {

    @RuntimeType
    fun intercept(
        @FieldValue(DRILL_DELEGATE_FIELD) target: Any,
        @FieldValue(DRILL_REQUEST_FIELD) drillRequest: DrillRequest,
        @FieldValue(DRILL_CONTEXT_FIELD) drillContext: Any,
        @Origin superMethod: Method,
        @Pipe pipe: Function<Any?, Any?>,
        @AllArguments args: Array<Any?>,
    ): Any? {
        return when (superMethod.name) {
            "onSubscribe" -> onSubscribe(target, drillRequest, superMethod, args[0], pipe)
            "currentContext" -> currentContext(target, drillRequest, drillContext, superMethod)
            "onNext", "onComplete", "onError" -> onOtherMethods(target, drillRequest, superMethod, pipe)
            else -> pipe.apply(target)
        }
    }

    private fun onSubscribe(
        target: Any,
        drillRequest: DrillRequest,
        superMethod: Method,
        subscription: Any?,
        pipe: Function<Any?, Any?>
    ): Any? {
        if (subscription == null)
            return pipe.apply(target)
        val subscriptionProxy = createProxyDelegate(
            subscription,
            Class.forName(SUBSCRIPTION_CLASS, true, target.javaClass.classLoader),
            SubscriptionInterceptor,
            configure = { defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC) },
            initialize = { proxy, proxyType -> proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, drillRequest) }
        )

        return propagateDrillRequest(drillRequest) {
            logger.trace { "${target.javaClass.simpleName}.onSubscribe(${subscription.javaClass.name}):${target.hashCode()}, sessionId = ${drillRequest.drillSessionId}" }
            superMethod.invoke(target, subscriptionProxy)
        }
    }

    private fun currentContext(
        target: Any,
        drillRequest: DrillRequest,
        drillContext: Any,
        superMethod: Method
    ): Any {
        return drillContext
    }

    private fun onOtherMethods(
        target: Any,
        drillRequest: DrillRequest,
        superMethod: Method,
        pipe: Function<Any?, Any?>,
    ): Any? {
        return propagateDrillRequest(drillRequest) {
            logger.trace { "${target.javaClass.simpleName}.${superMethod.name}():${target.hashCode()}, sessionId = ${drillRequest.drillSessionId}" }
            pipe.apply(target)
        }
    }
}