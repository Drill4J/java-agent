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

import com.epam.drill.agent.instrument.reactor.DRILL_DELEGATE_FIELD
import com.epam.drill.agent.instrument.reactor.DRILL_REQUEST_FIELD
import com.epam.drill.agent.instrument.reactor.propagateDrillRequest
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import mu.KotlinLogging
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.function.Function

/**
 * The Byte buddy method interceptor object for the {@link org.reactivestreams.Subscription} class.
 */
class SubscriptionInterceptor(
    private val requestHolder: RequestHolder
) {
    private val logger = KotlinLogging.logger {}


    /**
     * Intercepts all public methods of the {@link org.reactivestreams.Subscription} class, facilitating the propagation of the Drill context.
     * Within the `request()` and `cancel()` methods, it ensures the seamless propagation of the Drill context.
     * In other methods, it invokes the corresponding delegate method.
     * @param target the delegate of the {@link org.reactivestreams.Subscription} class.
     * @param drillRequest the value of the Drill Request which is located in field `DRILL_REQUEST_FIELD` in the {@link org.reactivestreams.Subscription} proxy class.
     * @param superMethod the name of the intercepted method.
     * @param pipe the Byte buddy method interceptor object.
     * @return the result of the intercepted method.
     */
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
        return propagateDrillRequest(drillRequest, requestHolder) {
            logger.trace { "${target.javaClass.simpleName}.${superMethod.name}():${target.hashCode()}, sessionId = ${drillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
            pipe.apply(target)
        }
    }
}