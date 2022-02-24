package com.epam.drill.agent.instrument.netty

import com.epam.drill.agent.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.*
import com.epam.drill.request.*
import io.opentelemetry.api.trace.*
import java.util.concurrent.atomic.*
import kotlin.reflect.jvm.*

object NettyStorage {
    val logger = Logging.logger(NettyStorage::class.jvmName)
    val regex = "^0*$".toRegex()

    private val drillHeaders: AtomicReference<Map<String, DrillRequest>> = AtomicReference(emptyMap())

    fun storeDrillHeaders(headers: Map<String, String>?) = run {
        headers?.get(
            HeadersRetriever.sessionHeaderPattern() ?: DRILL_SESSION_ID_HEADER_NAME
        )?.let { drillSessionId ->
            val drillHeaders = headers.filter { it.key.startsWith(DRILL_HEADER_PREFIX) }
            logger.trace { "for drillSessionId '$drillSessionId' store drillHeaders '$drillHeaders' to thread storage" }
            headerKey().takeIf { !it.matches(regex) }?.let { key ->
                this.drillHeaders.updateAndGet {
                    it + (key to DrillRequest(drillSessionId, drillHeaders))
                }
                PluginExtension.processServerRequest()
            }
        }
    }

    fun clearDrillHeaders() {
        drillHeaders.updateAndGet { it - headerKey() }
    }

    fun headerKey(): String = Span.current().spanContext.traceId

    fun loadDrillHeaders() = drillHeaders.get()[headerKey()]?.headers ?: emptyMap()

    fun getHeaders() = drillHeaders.get()[headerKey()]
}
