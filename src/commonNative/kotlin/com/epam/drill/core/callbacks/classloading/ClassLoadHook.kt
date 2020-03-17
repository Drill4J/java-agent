package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.core.plugin.loader.*
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
    val classBytesInJBytesArray: jbyteArray = NewByteArray(classDataLen)!!
    val readBytes = classData!!.readBytes(classDataLen)
    classBytesInJBytesArray.toByteArrayWithRelease(readBytes) {
        SetByteArrayRegion(classBytesInJBytesArray, 0, classDataLen, it.refTo(0))
        val jClassName = NewStringUTF(kClassName)
        val instrumentedBytes: jbyteArray? =
            CallObjectMethod(requestHolder, retrieveClassesData, jClassName, classBytesInJBytesArray, loader)
        DeleteLocalRef(jClassName)
        if (instrumentedBytes != null) {
            logger.debug { kClassName }
            instrumentedBytes.toByteArrayWithRelease {
                convertToNativePointers(it, newData, newClassDataLen)
            }
        } else {
            exec { pstorage.values.filterIsInstance<InstrumentationNativePlugin>() }.forEach { instrumentedPlugin ->
                instrumentedPlugin.instrument(kClassName!!, classData.readBytes(classDataLen))
                    ?.let { instrumentedBytes -> convertToNativePointers(instrumentedBytes, newData, newClassDataLen) }
            }
        }
    }
    DeleteLocalRef(classBytesInJBytesArray)
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

inline fun jbyteArray.toByteArrayWithRelease(bytes: ByteArray = byteArrayOf(), block: (ByteArray) -> Unit) {
    val nativeArray = GetByteArrayElements(this, null)?.apply {
        bytes.forEachIndexed { index, byte -> this[index] = byte }
    }
    try {
        block(nativeArray!!.readBytes(GetArrayLength(this)))
    } finally {
        ReleaseByteArrayElements(this, nativeArray, JNI_ABORT)
    }
}


private fun isNotSuitableClass(name: String?, data: CPointer<UByteVar>?, loader: jobject?, protectionDomain: jobject?) =
    (isSyntheticClass(name, data) || isBootstrapClassLoading(loader, protectionDomain))


private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

private fun isSyntheticClass(kClassName: String?, classData: CPointer<UByteVar>?) =
    kClassName == null || classData == null

