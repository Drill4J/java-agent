package com.epam.drill.request

import com.epam.drill.agent.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.*
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.*

actual object RequestHolder {

    fun get(): DrillRequest? {
        return dump()?.let { ProtoBuf.load(DrillRequest.serializer(), it) }
    }

    actual fun dump(): ByteArray? {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val drillRequest = GetMethodID(requestHolderClass, RequestHolder::dump.name, "()[B")
        return CallObjectMethod(requestHolder, drillRequest).readBytes()
    }


    actual fun store(drillRequest: ByteArray) {
        val (requestHolderClass, requestHolder: jobject?) = instance<RequestHolder>()
        val storeRequest =
            GetMethodID(requestHolderClass, RequestHolder::store.name, "([B)V")
        val jClassBytes: jbyteArray = NewByteArray(drillRequest.size)!!
        SetByteArrayRegion(jClassBytes, 0, drillRequest.size, drillRequest.refTo(0))
        CallVoidMethod(
            requestHolder,
            storeRequest,
            jClassBytes
        )
    }

    fun storeRequestMetadata(request: DrillRequest) {
        store(ProtoBuf.dump(DrillRequest.serializer(), request))
    }


}