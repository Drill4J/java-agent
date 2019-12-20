@file:Suppress("unused")

package com.epam.drill.ws

import com.alibaba.ttl.*
import com.epam.drill.plugin.*
import com.epam.drill.session.DrillRequest
import java.lang.Exception
import java.util.logging.*

object RequestHolder {

    private var sessionIdHeaderName: String = ""
    private val log = Logger.getLogger(RequestHolder::class.java.name)

    init {
        DrillRequest.threadStorage = TransmittableThreadLocal<com.epam.drill.plugin.DrillRequest>()
    }

    fun storeRequest(rawRequest: String, pattern: String?) {
        var drillRequest = parseHttpRequest(rawRequest).toDrillRequest()
        if (pattern != null) {
            val customId = drillRequest.headers[pattern] ?: run {
                try {
                    val groupValues = pattern.toRegex().find(rawRequest)?.groupValues
                    if (groupValues.isNullOrEmpty()) null else groupValues[1]
                } catch (ex: Exception) {
                    null
                }
            }
            if (customId != null)
                drillRequest = drillRequest.copy(drillSessionId = customId)
        }
        if (drillRequest.drillSessionId == null) {
            DrillRequest.threadStorage.remove()
        } else {
            DrillRequest.threadStorage.set(drillRequest)
            println("session saved: ${drillRequest.drillSessionId}")
        }
    }

    fun storeSessionId(sessionId: String) {
        this.sessionIdHeaderName = sessionId
    }

    fun request() = DrillRequest.threadStorage.get() ?: null
    fun sessionId(): String? {
        return DrillRequest.threadStorage.get()?.drillSessionId
    }


}
