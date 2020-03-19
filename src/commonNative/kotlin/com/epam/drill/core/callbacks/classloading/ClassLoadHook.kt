@file:Suppress("UNUSED_VARIABLE")

package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.core.callbacks.vminit.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import mu.*
import org.objectweb.asm.*

@SharedImmutable
private val logger = KotlinLogging.logger("jvmtiEventClassFileLoadHookEvent")

@SharedImmutable
private val directTtlClasses = listOf(
    "java/util/concurrent/ScheduledThreadPoolExecutor",
    "java/util/concurrent/ThreadPoolExecutor",
    "java/util/concurrent/ForkJoinTask",
    "java/util/concurrent/ForkJoinPool"
)

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
    checkNotNull(kClassName)
    val classReader = ClassReader(classData!!.readBytes(classDataLen))
    if (classReader.interfaces.contains("javax/servlet/ServletContextListener")) {
        val classBytesInJBytesArray: jbyteArray = NewByteArray(classDataLen)!!
        val readBytes = classData.readBytes(classDataLen)
        classBytesInJBytesArray.toByteArrayWithRelease(readBytes) {
            SetByteArrayRegion(classBytesInJBytesArray, 0, classDataLen, it.refTo(0))
            val jClassName = NewStringUTF(kClassName)
            val instrumentedBytes: jbyteArray? =
                CallObjectMethod(
                    transformerObject.value,
                    transformMethod.value,
                    jClassName,
                    classBytesInJBytesArray,
                    loader
                )
            DeleteLocalRef(jClassName)
            if (instrumentedBytes != null) {
                logger.error { kClassName }
                instrumentedBytes.toByteArrayWithRelease { convertToNativePointers(it, newData, newClassDataLen) }
            }
        }
        DeleteLocalRef(classBytesInJBytesArray)
    } else {
        if (directTtlClasses.contains(kClassName) || (kClassName != "java/util/TimerTask" && classReader.interfaces.contains(
                "java/lang/Runnable"
            )) || classReader.superName == "java/util/concurrent/ThreadPoolExecutor"
        ) {
            val classBytesInJBytesArray: jbyteArray = NewByteArray(classDataLen)!!
            val readBytes = classData.readBytes(classDataLen)
            classBytesInJBytesArray.toByteArrayWithRelease(readBytes) {
                SetByteArrayRegion(classBytesInJBytesArray, 0, classDataLen, it.refTo(0))
                val jClassName = NewStringUTF(kClassName)
                val instrumentedBytes: jbyteArray? =
                    CallObjectMethod(
                        ttlTransformerObject.value,
                        ttlTransformMethod.value,
                        loader,
                        jClassName,
                        classBeingRedefined,
                        protection_domain,
                        classBytesInJBytesArray,
                        loader
                    )
                DeleteLocalRef(jClassName)
                if (instrumentedBytes != null) {
                    println("$kClassName transformed...")
                    instrumentedBytes.toByteArrayWithRelease { convertToNativePointers(it, newData, newClassDataLen) }
                }
            }
            DeleteLocalRef(classBytesInJBytesArray)
        }
        exec { pstorage.values.filterIsInstance<InstrumentationNativePlugin>() }.forEach { instrumentedPlugin ->
            instrumentedPlugin.instrument(kClassName, classData.readBytes(classDataLen))
                ?.let { instrumentedBytes -> convertToNativePointers(instrumentedBytes, newData, newClassDataLen) }
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

