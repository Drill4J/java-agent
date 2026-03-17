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
 * The Byte buddy method interceptor object for the {@link reactor.core.publisher.Flux} and {@link reactor.core.publisher.Mono}.
 */
class PublisherInterceptor(
    private val requestHolder: RequestHolder,
) {
    private val logger = KotlinLogging.logger {}
    private val subscriberInterceptor = SubscriberInterceptor(requestHolder)

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
        val subscriberClass = Class.forName(SUBSCRIBER_CLASS, true, target.javaClass.classLoader)
        if (!subscriberClass.isAssignableFrom(subscriber::class.java)) return pipe.apply(target)

        val context = subscriber.getCurrentContext()
        val drillRequestFromContext = context.getOrDefault(DRILL_CONTEXT_KEY, null)

        //It is necessary to obtain the test context either from the subscriber context or from the current thread
        val parentDrillRequest = drillRequestFromContext
            ?: drillRequest
            ?: return pipe.apply(target)

        //If the test context isn't already in the subscriber context, it needs to be added there.
        val newContext = if (parentDrillRequest != drillRequestFromContext) {
            context.put(DRILL_CONTEXT_KEY, parentDrillRequest)
        } else context

        val subscriberProxy = createProxyDelegate(
            subscriber,
            subscriberClass,
            subscriberInterceptor,
            configure = {
                defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC)
                    .defineField(DRILL_CONTEXT_FIELD, Object::class.java, Visibility.PUBLIC)
            },
            initialize = { proxy, proxyType ->
                proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, parentDrillRequest)
                proxyType.getField(DRILL_CONTEXT_FIELD).set(proxy, newContext)
            }
        )
        return propagateDrillRequest(parentDrillRequest, requestHolder) {
            logger.trace { "${target.javaClass.simpleName}.${superMethod.name}():${target.hashCode()}, sessionId = ${parentDrillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
            superMethod.invoke(target, subscriberProxy)
        }
    }

    /**
     * Calls the {@link reactor.util.context.Context#getOrDefault(Object, Object)} method and returns the result.
     * @receiver a context {@link reactor.util.context.Context}
     * @param key the key of the context
     * @param defaultValue the default value
     * @return the result of calling the {@link reactor.util.context.Context#getOrDefault(Object, Object)} method
     */
    private fun Any.getOrDefault(key: String, defaultValue: Any?): DrillRequest? {
        val getOrDefaultMethod = this.javaClass.getMethod("getOrDefault", Any::class.java, Any::class.java)
        getOrDefaultMethod.isAccessible = true
        return getOrDefaultMethod.invoke(this, key, defaultValue) as DrillRequest?
    }

    /**
     * Calls the {@link reactor.core.CoreSubscriber#currentContext()} method and returns the result.
     * @receiver a subscriber {@link reactor.core.CoreSubscriber}
     * @return a
     */
    private fun Any.getCurrentContext(): Any {
        val currentContextMethod = this.javaClass.getMethod("currentContext")
        currentContextMethod.isAccessible = true
        return currentContextMethod.invoke(this)
    }

    /**
     * Calls the {@link reactor.util.context.Context#put(Object, Object)} method and returns the result.
     * @receiver a context {@link reactor.util.context.Context}
     * @param key the key of the context
     * @param value the value of the context
     * @return the result of calling the {@link reactor.util.context.Context#put(Object, Object)} method
     */
    private fun Any.put(key: String, value: Any): Any {
        val putMethod = this.javaClass.getMethod("put", Any::class.java, Any::class.java)
        putMethod.isAccessible = true
        return putMethod.invoke(this, key, value)
    }
}