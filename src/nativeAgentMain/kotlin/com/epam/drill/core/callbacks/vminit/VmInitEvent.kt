@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.*
import com.epam.drill.core.agent.*
import com.epam.drill.core.methodbind.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

@SharedImmutable
val wsThread = Worker.start(true)

@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    configureHttp()
    wsThread.execute(TransferMode.UNSAFE, {}) {
        calculateBuildVersion()
        WsSocket().connect(exec { adminAddress.toString() })
    }
}

