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

import com.epam.drill.agent.instrument.reactor.interceptors.PublisherInterceptor
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import mu.KotlinLogging
import net.bytebuddy.description.modifier.Visibility

private val logger = KotlinLogging.logger {}

/**
 * The object responsible for creating proxy delegates instances of publisher classes.
 */
object PublisherAssembler {
    /**
     * Creates proxy delegate for the given publisher class.
     * @param target the delegate instance.
     * @param publisherClass the publisher class. Must be a superclass of the delegate class
     * @return the proxy delegate instance
     */
    @JvmStatic
    fun onAssembly(
        target: Any,
        publisherClass: Class<*>,
        requestHolder: RequestHolder
    ): Any {
        val drillRequest = requestHolder.retrieve()
        logger.trace { "${publisherClass.simpleName}.onAssembly(${target.javaClass.simpleName}):${target.hashCode()}, sessionId = ${drillRequest?.drillSessionId}, threadId = ${Thread.currentThread().id}" }
        return createProxyDelegate(
            target,
            publisherClass,
            PublisherInterceptor(requestHolder),
            configure = { defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC) },
            initialize = { proxy, proxyType ->
                if (drillRequest != null)
                    proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, drillRequest)
            }
        )
    }
}