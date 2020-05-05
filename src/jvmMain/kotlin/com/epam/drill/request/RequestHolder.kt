package com.epam.drill.request

import com.alibaba.ttl.*
import com.epam.drill.logging.*
import com.epam.drill.plugin.*
import com.epam.drill.session.*
import kotlinx.serialization.protobuf.*

actual object RequestHolder {

    private var sessionIdHeaderName: String = ""

    init {
        threadStorage = TransmittableThreadLocal()
    }

    actual fun store(rawRequest: String, pattern: String?) {
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
            log(Level.INFO) { "session saved: ${drillRequest.drillSessionId}" }
        }
    }

    @Suppress("unused")
    fun storeSessionId(sessionId: String) {
        this.sessionIdHeaderName = sessionId
    }

    @Suppress("unused")
    fun request() = threadStorage.get() ?: null

    actual fun dump(): ByteArray? {
        return threadStorage.get()?.let { ProtoBuf.dump(DrillRequest.serializer(), it) }
    }

}
