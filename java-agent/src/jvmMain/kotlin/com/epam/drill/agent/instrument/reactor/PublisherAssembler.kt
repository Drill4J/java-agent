package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.request.DrillRequest
import mu.KotlinLogging
import net.bytebuddy.description.modifier.Visibility

private val logger = KotlinLogging.logger {}

/**
 * Object for creation proxy delegates instances of publisher classes.
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
        publisherClass: Class<*>
    ): Any {
        val drillRequest = RequestHolder.retrieve()
        logger.trace { "${publisherClass.simpleName}.onAssembly(${target.javaClass.simpleName}):${target.hashCode()}, sessionId = ${drillRequest?.drillSessionId}" }
        return createProxyDelegate(
            target,
            publisherClass,
            PublisherInterceptor,
            configure = { defineField(DRILL_REQUEST_FIELD, DrillRequest::class.java, Visibility.PUBLIC) },
            initialize = { proxy, proxyType ->
                if (drillRequest != null)
                    proxyType.getField(DRILL_REQUEST_FIELD).set(proxy, drillRequest)
            }
        )
    }
}