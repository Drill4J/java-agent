package com.epam.drill.request

import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*

internal class RequestAgentContext(
    private val requestProvider: () -> DrillRequest?
) : AgentContext {
    override operator fun invoke(): String? = requestProvider()?.drillSessionId?.ifEmpty { null }
    override operator fun get(key: String): String? = requestProvider()?.headers?.get(key.toLowerCase())
}
