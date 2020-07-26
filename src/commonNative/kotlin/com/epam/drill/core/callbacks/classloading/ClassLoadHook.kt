@file:Suppress("UNUSED_VARIABLE")

package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.instrument.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import org.objectweb.asm.*

@SharedImmutable
private val logger = Logging.logger("jvmtiEventClassFileLoadHookEvent")

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
    try {
        val classBytes = ByteArray(classDataLen).apply {
            Memory.of(classData, classDataLen).loadByteArray(0, this)
        }
        val classReader = ClassReader(classBytes)
        val transformers = mutableListOf<() -> ByteArray?>()
        if (!state.allWebAppsInitialized() && "javax/servlet/ServletContextListener" in classReader.interfaces)
            transformers += { Transformer.transform(kClassName, classBytes, loader) }
        if (kClassName in directTtlClasses || kClassName != "java/util/TimerTask" && "java/lang/Runnable" in classReader.interfaces || classReader.superName == "java/util/concurrent/ThreadPoolExecutor")
            transformers += {
                TTLTransformer.transform(
                    loader,
                    kClassName,
                    classBeingRedefined,
                    classBytes
                )
            }
        if ("$" !in kClassName && state.packagePrefixes.any { kClassName.startsWith(it) })
            pstorage.values.filterIsInstance<InstrumentationNativePlugin>().forEach { plugin ->
                transformers += { plugin.instrument(kClassName, classBytes) }
            }
        if (transformers.any()) {
            transformers.fold(classBytes) { jbytes, transformer -> transformer() ?: jbytes }
                .takeIf { it !== classBytes }?.let {
                    logger.trace { "$kClassName transformed" }
                    convertToNativePointers(it, newData, newClassDataLen)
                }
        }
    } catch (throwable: Throwable) {
        logger.error(throwable) {
            "Can't retransform class: $kClassName, ${classData.readBytes(classDataLen).contentToString()}"
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

private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

