@file:Suppress("unused")

package com.epam.drill.request

import com.alibaba.ttl.*
import com.epam.drill.plugin.*
import com.epam.drill.session.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import java.util.logging.*

actual object RequestHolder {

    init {
        threadStorage = TransmittableThreadLocal()
    }

    private var sessionIdHeaderName: String = ""
    private val log = Logger.getLogger(RequestHolder::class.java.name)

    actual fun storeRequest(rawRequest: String, pattern: String?){
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
            threadStorage.remove()
        } else {
            threadStorage.set(drillRequest)
            println("session saved: ${drillRequest.drillSessionId}")
        }
    }

    fun storeSessionId(sessionId: String) {
        this.sessionIdHeaderName = sessionId
    }

    fun request() = threadStorage.get() ?: null

    actual fun drillRequest(): Any? {
        return threadStorage.get()?.let { Cbor.dumps(DrillRequest.serializer(), it) }
    }

}
