@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.core.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*

private val logger = Logging.logger("VmInitEvent")

@Suppress("UNUSED_PARAMETER")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    configureHttp()
    globalCallbacks()
    WsSocket().connect(adminAddress.toString())
    RequestHolder.setAsyncMode(config.isAsyncApp)
    runBlocking {
        for (i in 1..5) {
            logger.info { "Agent is not alive. Waiting for package settings from $adminAddress..." }
            delay(500L)
            if (state.alive) {
                logger.info { "Agent is alive! Waiting for loading of at least one plugin..." }
                while (pstorage.none()) {
                    delay(500L)
                }
                logger.info {
                    "At least on plugin is loaded (plugins ${pstorage.keys.toList()}), continue vm initializing."
                }
                break
            }
        }
        if (pstorage.none()) {
            logger.info { "No plugins loaded from $adminAddress." }
        }
    }
}
