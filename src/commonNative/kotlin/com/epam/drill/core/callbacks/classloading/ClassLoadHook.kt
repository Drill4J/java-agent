@file:Suppress("UNUSED_VARIABLE")

package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.agent.*
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
    if (isBootstrapClassLoading(loader, protection_domain)) return
    val kClassName = clsName?.toKString()
    if (kClassName == null || classData == null || kClassName.startsWith("com/epam/drill")) return
    val classBytes = classData.readBytes(classDataLen)
    val classReader = ClassReader(classBytes)
    val transformers = mutableListOf<(jstring, jbyteArray) -> jbyteArray?>()
    if (!state.isWebAppInitialized &&
        "javax/servlet/ServletContextListener" in classReader.interfaces
    ) transformers += { jname, jbytes ->
        CallObjectMethod(
            transformerObject.value,
            transformMethod.value,
            jname,
            jbytes,
            loader
        )
    }
    if (kClassName in directTtlClasses ||
        kClassName != "java/util/TimerTask" && "java/lang/Runnable" in classReader.interfaces ||
        classReader.superName == "java/util/concurrent/ThreadPoolExecutor"
    ) transformers += { jname, jbytes ->
        CallObjectMethod(
            ttlTransformerObject.value,
            ttlTransformMethod.value,
            loader,
            jname,
            classBeingRedefined,
            protection_domain,
            jbytes,
            loader
        )
    }
    if (state.packagePrefixes.any { kClassName.startsWith(it) }) exec {
        pstorage.values.filterIsInstance<InstrumentationNativePlugin>()
    }.forEach { plugin ->
        transformers += { jname, jbytes ->
            CallObjectMethod(
                plugin.userPlugin,
                plugin.qs,
                jname,
                jbytes
            )
        }
    }
    if (transformers.any()) {
        val jClassBytes: jbyteArray = NewByteArray(classDataLen)!!
        val jClassName: jstring = NewStringUTF(kClassName)!!
        try {
            jClassBytes.toByteArrayWithRelease(classBytes) { kClassBytes ->
                SetByteArrayRegion(jClassBytes, 0, classDataLen, kClassBytes.refTo(0))
                transformers.fold(jClassBytes) { jbytes, transformer ->
                    transformer(jClassName, jbytes) ?: jbytes
                }.takeIf { it !== jClassBytes }
                    ?.toByteArrayWithRelease {
                        logger.debug { "$kClassName transformed" }
                        convertToNativePointers(it, newData, newClassDataLen)
                    }
            }
        } finally {
            DeleteLocalRef(jClassBytes)
            DeleteLocalRef(jClassName)
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

inline fun jbyteArray.toByteArrayWithRelease(
    bytes: ByteArray = byteArrayOf(),
    block: (ByteArray) -> Unit
) {
    val nativeArray = GetByteArrayElements(this, null)?.apply {
        bytes.forEachIndexed { index, byte -> this[index] = byte }
    }
    try {
        block(nativeArray!!.readBytes(GetArrayLength(this)))
    } finally {
        ReleaseByteArrayElements(this, nativeArray, JNI_ABORT)
    }
}


private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

