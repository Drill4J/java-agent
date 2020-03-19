@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

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
    WsSocket().connect(exec { adminAddress.toString() })
}

private fun initializeTtlTransformer() {
    val (clazz, instance: jobject?) = instance("com/epam/drill/agent/instrument/TTLTransformer")
    _ttlTransformerObject.value = NewGlobalRef(instance).freeze()
    val method: CPointer<_jmethodID>? =
        GetMethodID(
            clazz,
            "transform",
            "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B"
        )
    _ttlTranformMethod.value = method.freeze()
}

private fun initializeMainTransformer() {
    val (clazz, instance: jobject?) = instance("com/epam/drill/agent/instrument/Transformer")
    _transformerObject.value = NewGlobalRef(instance).freeze()
    val method: CPointer<_jmethodID>? =
        GetMethodID(clazz, "transform", "(Ljava/lang/String;[BLjava/lang/ClassLoader;)[B")
    _tranformMethod.value = method.freeze()
}

