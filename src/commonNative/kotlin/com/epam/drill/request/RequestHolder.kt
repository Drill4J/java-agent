package com.epam.drill.request

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.*
import kotlinx.serialization.cbor.*

actual object RequestHolder {

    fun get(): DrillRequest? {
        return dump()?.let { Cbor.load(DrillRequest.serializer(), it) }
    }

    actual fun dump(): ByteArray? {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val drillRequest = GetMethodID(requestHolderClass, RequestHolder::dump.name, "()[B")
        return CallObjectMethod(requestHolder, drillRequest).readBytes()
    }


    actual fun store(rawRequest: String, pattern: String?) {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val storeRequest = GetMethodID(requestHolderClass, RequestHolder::store.name, "(Ljava/lang/String;Ljava/lang/String;)V")
        CallVoidMethod(requestHolder, storeRequest, NewStringUTF(rawRequest), NewStringUTF(exec { this.requestPattern }))
    }


}