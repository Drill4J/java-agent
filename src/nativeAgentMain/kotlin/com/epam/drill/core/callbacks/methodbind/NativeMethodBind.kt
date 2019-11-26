package com.epam.drill.core.callbacks.methodbind

import com.epam.drill.core.methodbind.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventNativeMethodBindEvent")
fun nativeMethodBind(
    jvmtiEnv: jvmtiEnv,
    jniEnv: JNIEnv,
    thread: jthread,
    method: jmethodID,
    address: COpaquePointer,
    newAddressPtr: CPointer<COpaquePointerVar>
) {
    nativeMethodBindMapper[method.getDeclaringClassName() + method.getName()]?.let {
        println(method.getDeclaringClassName() + method.getName() + " proxied.")
        newAddressPtr.pointed.value = it(address)
    }
}
