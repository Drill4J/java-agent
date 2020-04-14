package com.epam.drill.request

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

actual object RequestHolder {

    actual fun drillRequest(): Any? {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val drillRequest = GetMethodID(requestHolderClass, RequestHolder::drillRequest.name, "()Ljava/lang/Object;")
        val jRawString = CallObjectMethod(requestHolder, drillRequest)
        return jRawString?.let { Cbor.loads(DrillRequest.serializer(), it.toKString()!!) }
    }


    actual fun storeRequest(rawRequest: String, pattern: String?) {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val storeRequest = GetMethodID(requestHolderClass, RequestHolder::storeRequest.name, "(Ljava/lang/String;Ljava/lang/String;)V")
        CallVoidMethod(requestHolder, storeRequest, NewStringUTF(rawRequest), NewStringUTF(exec { this.requestPattern }))
    }


}