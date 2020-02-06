package com.epam.drill.core.methodbind

import com.epam.drill.core.*
import com.epam.drill.hook.http.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import mu.*

@SharedImmutable
val httpRequestLogger = KotlinLogging.logger("http requestLogger")


fun configureHttp() {
    configureHttpHooks()

    addHttpReadCallbacks { bytes ->
        bytes?.let {
            fillRequestToHolder(it.decodeToString())
        }
    }

    addHttpWriteCallback {
        val agentId = generateId()
        val adminUrl = retrieveAdminUrl()
        val sessionId = sessionId()
        mapOf(
            "drill-agent-id" to agentId,
            "drill-admin-url" to adminUrl
        ) + if (sessionId != null)
            mapOf("drill-session-id" to sessionId)
        else emptyMap()
    }

}

private fun retrieveAdminUrl(): String {
    return exec {
        if (::secureAdminAddress.isInitialized) {
            secureAdminAddress.toUrlString(false)
        } else adminAddress.toUrlString(false)
    }.toString()
}

private fun generateId() =
    exec { if (agentConfig.serviceGroupId.isEmpty()) agentConfig.id else agentConfig.serviceGroupId }

fun fillRequestToHolder(request: String) {
    val requestPattern = exec { requestPattern }
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val retrieveClassesData = GetMethodID(requestHolderClass, "storeRequest", "(Ljava/lang/String;Ljava/lang/String;)V")
    CallVoidMethod(requestHolder, retrieveClassesData, NewStringUTF(request), NewStringUTF(requestPattern))
}

fun sessionId(): String? {
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val retrieveClassesData = GetMethodID(requestHolderClass, "sessionId", "()Ljava/lang/String;")
    val jRawString = CallObjectMethod(requestHolder, retrieveClassesData) ?: return null
    return jRawString.toKString()
}

private fun instance(name: String): Pair<jclass?, jobject?> {
    val requestHolderClass = FindClass(name)
    val selfMethodId: jfieldID? = GetStaticFieldID(requestHolderClass, "INSTANCE", "L$name;")
    val requestHolder: jobject? = GetStaticObjectField(requestHolderClass, selfMethodId)
    return Pair(requestHolderClass, requestHolder)
}