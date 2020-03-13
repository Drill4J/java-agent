package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.core.transport.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import mu.*
import kotlin.native.concurrent.*


@SharedImmutable
private val logger = KotlinLogging.logger("jvmtiEventClassFileLoadHookEvent")

@Suppress("unused", "UNUSED_PARAMETER")
fun classLoadEvent(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    clsName: CPointer<ByteVar>?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?
) {
    initRuntimeIfNeeded()
    val kClassName = clsName?.toKString()
    if (isNotSuitableClass(kClassName, classData, loader, protection_domain)) return


    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/agent/instrument/Transformer")
    val retrieveClassesData =
        GetMethodID(requestHolderClass, "transform", "(Ljava/lang/String;[BLjava/lang/ClassLoader;)[B")
    val newByteArray: jbyteArray? = NewByteArray(classDataLen)
    SetByteArrayRegion(newByteArray, 0, classDataLen, getBytes(newByteArray, classData!!.readBytes(classDataLen)))

    val instrumentedBytes: jbyteArray? =
        CallObjectMethod(requestHolder, retrieveClassesData, NewStringUTF(kClassName), newByteArray, loader)

    if (instrumentedBytes != null) {
        logger.debug { kClassName }
        convertToNativePointers(instrumentedBytes.toByteArray(), newData, newClassDataLen)
    } else {
        exec { pstorage.values.filterIsInstance<InstrumentationNativePlugin>() }.forEach { instrumentedPlugin ->
            instrumentedPlugin.instrument(kClassName!!, classData.readBytes(classDataLen))?.let { instrumentedBytes ->
                convertToNativePointers(instrumentedBytes, newData, newClassDataLen)
            }
        }
    }
}

private fun convertToNativePointers(
    instrumentedBytes: ByteArray,
    newData: CPointer<CPointerVar<UByteVar>>?,
    newClassDataLen: CPointer<jintVar>?
) {
    val instrumentedSize = instrumentedBytes.size
    Allocate(instrumentedSize.toLong(), newData)
    instrumentedBytes.forEachIndexed { index, byte ->
        val innerValue = newData!!.pointed.value!!
        innerValue[index] = byte.toUByte()
    }
    newClassDataLen!!.pointed.value = instrumentedSize
}

private fun jobject.toByteArray() = GetByteArrayElements(this, null)!!.readBytes(GetArrayLength(this))

private fun getBytes(newByteArray: jbyteArray?, classData: ByteArray) =
    GetByteArrayElements(newByteArray, null)?.apply {
        classData.forEachIndexed { index, byte ->
            this[index] = byte
        }
    }


private fun isNotSuitableClass(name: String?, data: CPointer<UByteVar>?, loader: jobject?, protectionDomain: jobject?) =
    (isSyntheticClass(name, data) || isBootstrapClassLoading(loader, protectionDomain))


private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

private fun isSyntheticClass(kClassName: String?, classData: CPointer<UByteVar>?) =
    kClassName == null || classData == null

