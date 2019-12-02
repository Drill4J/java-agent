@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.plugin.*
import com.epam.drill.session.DrillRequest
import java.util.logging.*

object RequestHolder {

    private var sessionIdHeaderName: String = ""
    private val log = Logger.getLogger(RequestHolder::class.java.name)

    fun storeRequest(rawRequest: String, id: String?) {
        var toDrillRequest = parseHttpRequest(rawRequest).toDrillRequest()
        if (id != null) toDrillRequest = toDrillRequest.copy(drillSessionId = id)
        if (toDrillRequest.drillSessionId == null) {
            DrillRequest.threadStorage.remove()
        } else {
            DrillRequest.threadStorage.set(toDrillRequest)
            println("session saved: ${toDrillRequest.drillSessionId}")
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
