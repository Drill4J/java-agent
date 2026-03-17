/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.instrument.reactor.interceptors

import com.epam.drill.agent.instrument.reactor.*
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import mu.KotlinLogging
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.function.Function

/**
 * The Byte buddy method interceptor object for the {@link reactor.core.CoreSubscriber} class.
 */
class SubscriberInterceptor(
    private val requestHolder: RequestHolder
) {
    private val logger = KotlinLogging.logger {}
    private val subscriptionInterceptor = SubscriptionInterceptor(requestHolder)

    /**
     * Intercepts all public methods of {@link reactor.core.CoreSubscriber} class and propagates the Drill Request.
     * In the `onSubscribe()` method it changes {@link org.reactivestreams.Subscription} argument on proxy copy and propagates the Drill Request.
     * In the `currentContext()` method it returns {drillContext} object as a {@link reactor.util.context.Context}.
     * In the `onNext`, `onComplete`, `onError` methods it propagates the Drill Request.
     * In other methods it calls the corresponding delegate method.
     * @param target the delegate of the {@link reactor.core.CoreSubscriber} class.
     * @param drillRequest the value of the Drill Request which is located in field `DRILL_REQUEST_FIELD` in the {@link reactor.core.CoreSubscriber} proxy class.
     * @param superMethod the name of the intercepted method.
     * @param pipe the Byte buddy method interceptor object.
     * @param args the arguments of the intercepted method.
     * @return the result of the intercepted method.
     */
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
            "currentContext" -> currentContext(drillContext)
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
            subscriptionInterceptor,
            configure = { defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC) },
            initialize = { proxy, proxyType -> proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, drillRequest) }
        )

        return propagateDrillRequest(drillRequest, requestHolder) {
            logger.trace { "${target.javaClass.simpleName}.onSubscribe(${subscription.javaClass.name}):${target.hashCode()}, sessionId = ${drillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
            superMethod.invoke(target, subscriptionProxy)
        }
    }

    private fun currentContext(drillContext: Any
    ): Any {
        return drillContext
    }

    private fun onOtherMethods(
        target: Any,
        drillRequest: DrillRequest,
        superMethod: Method,
        pipe: Function<Any?, Any?>,
    ): Any? {
        return propagateDrillRequest(drillRequest, requestHolder) {
            logger.trace { "${target.javaClass.simpleName}.${superMethod.name}():${target.hashCode()}, sessionId = ${drillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
            pipe.apply(target)
        }
    }
}