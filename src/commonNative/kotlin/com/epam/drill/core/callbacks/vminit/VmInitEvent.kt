@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import cnames.structs._jmethodID
import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.instrument.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

private val logger = Logging.logger("VmInitEvent")

private val _transformerObject = AtomicReference<jobject?>(null).freeze()
private val _tranformMethod = AtomicReference<CPointer<_jmethodID>?>(null).freeze()

private val _ttlTransformerObject = AtomicReference<jobject?>(null).freeze()
private val _ttlTranformMethod = AtomicReference<CPointer<_jmethodID>?>(null).freeze()

@ThreadLocal
val transformerObject = lazy {
    _transformerObject.value
}

@ThreadLocal
val transformMethod = lazy {
    _tranformMethod.value
}

@ThreadLocal
val ttlTransformerObject = lazy {
    _ttlTransformerObject.value
}

@ThreadLocal
val ttlTransformMethod = lazy {
    _ttlTranformMethod.value
}

@Suppress("UNUSED_PARAMETER")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    initializeMainTransformer()
    initializeTtlTransformer()

    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    configureHttp()
    WsSocket().connect(adminAddress.toString())
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

private fun initializeTtlTransformer() {
    val (clazz, instance: jobject?) = instance<TTLTransformer>()
    _ttlTransformerObject.value = NewGlobalRef(instance).freeze()
    val method: CPointer<_jmethodID>? =
        GetMethodID(
            clazz,
            TTLTransformer::transform.name,
            "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B"
        )
    _ttlTranformMethod.value = method.freeze()
}

private fun initializeMainTransformer() {
    val (clazz, instance: jobject?) = instance<Transformer>()
    _transformerObject.value = NewGlobalRef(instance).freeze()
    val method: CPointer<_jmethodID>? =
        GetMethodID(clazz, Transformer::transform.name, "(Ljava/lang/String;[BLjava/lang/ClassLoader;)[B")
    _tranformMethod.value = method.freeze()
}

