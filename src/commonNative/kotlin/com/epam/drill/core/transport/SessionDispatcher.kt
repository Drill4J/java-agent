package com.epam.drill.core.transport

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*

fun fillRequestToHolder(request: String) {
    val requestPattern = exec { requestPattern }
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val storeRequest = GetMethodID(requestHolderClass, "storeRequest", "(Ljava/lang/String;Ljava/lang/String;)V")
    CallVoidMethod(requestHolder, storeRequest, NewStringUTF(request), NewStringUTF(requestPattern))
}

fun sessionId(): String? {
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val sessionId = GetMethodID(requestHolderClass, "sessionId", "()Ljava/lang/String;")
    val jRawString = CallObjectMethod(requestHolder, sessionId) ?: return null
    return jRawString.toKString()
}
