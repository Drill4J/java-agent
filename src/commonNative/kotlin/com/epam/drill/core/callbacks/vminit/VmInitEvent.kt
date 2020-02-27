@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    configureHttp()
    WsSocket().connect(exec { adminAddress.toString() })
}

