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
package com.epam.drill.agent.instrument.reactor

import com.epam.drill.common.agent.request.DrillRequest
import mu.KotlinLogging
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.function.Function

private val logger = KotlinLogging.logger {}

/**
 * The Byte buddy method interceptor object for the {@link reactor.core.publisher.Flux} and {@link reactor.core.publisher.Mono}.
 */
object PublisherInterceptor {

    /**
     * Intercepts all public methods of {@link org.reactivestreams.Publisher} class and propagates the Drill Request.
     * In the `subscribe()` method it changes {@link reactor.core.CoreSubscriber} argument on proxy copy and propagates the Drill Request.
     * In other methods it calls the corresponding delegate method.
     * @param target the delegate of the {@link org.reactivestreams.Publisher} class.
     * @param drillRequest the value of the Drill Request which is located in field `DRILL_REQUEST_FIELD` in the {@link org.reactivestreams.Publisher} proxy class.
     * @param superMethod the name of the intercepted method.
     * @param pipe the Byte buddy method interceptor object.
     * @param args the arguments of the intercepted method.
     * @return the result of the intercepted method.
     */
    @RuntimeType
    fun intercept(
        @FieldValue(DRILL_DELEGATE_FIELD) target: Any,
        @FieldValue(DRILL_REQUEST_FIELD) drillRequest: DrillRequest?,
        @Origin superMethod: Method,
        @Pipe pipe: Function<Any?, Any?>,
        @AllArguments args: Array<Any?>,
    ): Any? {
        return when (superMethod.name) {
            "subscribe" -> subscribe(target, drillRequest, superMethod, pipe, args[0])
            else -> pipe.apply(target)
        }
    }

    private fun subscribe(
        target: Any,
        drillRequest: DrillRequest?,
        superMethod: Method,
        pipe: Function<Any?, Any?>,
        subscriber: Any?,
    ): Any? {
        if (subscriber == null) return pipe.apply(target)
        val context = getCurrentContext(subscriber)
        val contextualDrillRequest = getContext(context, DRILL_CONTEXT_KEY)

        val parentDrillRequest = contextualDrillRequest
            ?: drillRequest
            ?: return pipe.apply(target)

        val newContext = if (drillRequest != contextualDrillRequest) {
            putContext(context, DRILL_CONTEXT_KEY, parentDrillRequest)
        } else context

        val subscriberProxy = createProxyDelegate(
            subscriber,
            Class.forName(SUBSCRIBER_CLASS, true, target.javaClass.classLoader),
            SubscriberInterceptor,
            configure = {
                defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC)
                defineField(DRILL_CONTEXT_FIELD, Object::class.java, Visibility.PUBLIC)
            },
            initialize = { proxy, proxyType ->
                proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, parentDrillRequest)
                proxyType.getField(DRILL_CONTEXT_FIELD).set(proxy, newContext)
            }
        )
        return propagateDrillRequest(parentDrillRequest) {
            logger.trace { "${target.javaClass.simpleName}.${superMethod.name}():${target.hashCode()}, sessionId = ${drillRequest?.drillSessionId}" }
            superMethod.invoke(target, subscriberProxy)
        }
    }

    /**
     * Calls the {@link reactor.util.context.Context#getOrDefault(Object, Object)} method and returns the result.
     * @param context the value of the Context
     * @param key the key of the context
     */
    private fun getContext(context: Any, key: String): DrillRequest? {
        val getOrDefaultMethod = context.javaClass.getMethod("getOrDefault", Any::class.java, Any::class.java)
        getOrDefaultMethod.isAccessible = true
        return getOrDefaultMethod.invoke(context, key, null) as DrillRequest?
    }

    /**
     * Calls the {@link reactor.core.CoreSubscriber#currentContext()} method and returns the result.
     * @param subscriber the value of the CoreSubscriber
     */
    private fun getCurrentContext(subscriber: Any): Any {
        val currentContextMethod = subscriber.javaClass.getMethod("currentContext")
        currentContextMethod.isAccessible = true
        return currentContextMethod.invoke(subscriber)
    }

    /**
     * Calls the {@link reactor.util.context.Context#put(Object, Object)} method and returns the result.
     * @param context the value of the Context
     * @param key the key of the context
     * @param value the value of the context
     * @return the result of calling the {@link reactor.util.context.Context#put(Object, Object)} method
     */
    private fun putContext(context: Any, key: String, value: Any): Any {
        val putMethod = context.javaClass.getMethod("put", Any::class.java, Any::class.java)
        putMethod.isAccessible = true
        return putMethod.invoke(context, key, value)
    }
}