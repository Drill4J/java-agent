@file:Suppress("unused", "FunctionName")

package com.epam.drill.core

import com.epam.drill.agent.*
import com.epam.drill.api.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.time.*

@SharedImmutable
private val logger = Logging.logger("SymbolsRegister")

@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return com.epam.drill.jvmapi.currentEnvs()
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? {
    return com.epam.drill.jvmapi.jvmtii()
}

@CName("getJvm")
fun getJvm(): CPointer<JavaVMVar>? {
    return vmGlobal.value
}

@CName("JNI_OnUnload")
fun JNI_OnUnload() {
}

@CName("JNI_GetCreatedJavaVMs")
fun JNI_GetCreatedJavaVMs() {
}

@CName("JNI_CreateJavaVM")
fun JNI_CreateJavaVM() {
}

@CName("JNI_GetDefaultJavaVMInitArgs")
fun JNI_GetDefaultJavaVMInitArgs() {
}

@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_processing_Sender_sendMessage")
fun sendFromJava(envs: JNIEnv, thiz: jobject, jpluginId: jstring, jmessage: jstring) = withJSting {
    sendToSocket(jpluginId.toKString(), jmessage.toKString())
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_Native_RetransformClassesByPackagePrefixes")
fun RetransformClassesByPackagePrefixes(env: JNIEnv, thiz: jobject, prefixes: jbyteArray): jint = memScoped {
    val decodedPrefixes = prefixes.readBytes()?.decodePackages() ?: emptyList()
    val drillPackage = "Lcom/epam/drill"
    val allPrefixes = (state.packagePrefixes + decodedPrefixes).map { "L$it" }.filter {
        !it.startsWith(drillPackage)
    }.distinct()
    logger.info { "Package prefixes: $allPrefixes." }
    allPrefixes.takeIf { it.any() }?.let { prefixes ->
        getLoadedClasses().filter {
            it.status() in 0.toUInt()..7.toUInt()
        }.filter { jclass ->
            val signature = jclass.signature()
            '$' !in signature && !signature.startsWith(drillPackage) && prefixes.any { signature.startsWith(it) }
        }.partition { it.status() == 7.toUInt() }.let { (loaded, undetermined)  ->
            logger.info { "${loaded.size + undetermined.size} classes to retransform." }
            measureTimedValue {
                val processedCount: Int = measureTimedValue {
                    RetransformClasses(loaded.size, loaded.toCValues())
                }.let { (retCode, duration) ->
                    loaded.size.takeIf { retCode == 0.toUInt() }?.also {
                        logger.info { "Retransformed ${loaded.size} loaded classes in $duration." }
                    } ?: 0.also { logger.error { "Error retransforming loaded classes." } }
                }
                val failed: List<jclass> = loaded.takeIf { processedCount == 0 } ?: emptyList()
                (failed + undetermined).chunked(32) { chunk ->
                    if (RetransformClasses(chunk.size, chunk.toCValues()) != 0.toUInt()) {
                        logger.error { "Error retransforming chunk, starting per-class retransforming for the chunk." }
                        chunk.count { jclass ->
                            (RetransformClasses(1, cValuesOf(jclass)) == 0.toUInt()).also {
                                if (!it) logger.error {
                                    "Error retransforming ${jclass.signature()}, status=${jclass.status()}"
                                }
                            }
                        }
                    } else chunk.size
                }.sum().plus(processedCount)
            }.also {
                logger.info { "Retransformed ${it.value} classes in ${it.duration}." }
            }.value
        }
    } ?: 0
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_Native_RetransformClasses")
fun RetransformClasses(env: JNIEnv, thiz: jobject, count: jint, classes: jobjectArray) = memScoped {
    val allocArray = allocArray<jclassVar>(count) { index ->
        value = GetObjectArrayElement(classes, index)
    }
    RetransformClasses(count, allocArray)
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_Native_GetAllLoadedClasses")
fun GetAllLoadedClasses(env: JNIEnv, thiz: jobject) = memScoped {
    val loadedClasses = getLoadedClasses().toList()
    val javaArray = NewObjectArray(loadedClasses.size, FindClass("java/lang/Class"), null)

    for (i in loadedClasses.indices) {
        val cPointer = loadedClasses[i]
        SetObjectArrayElement(javaArray, i, cPointer)
    }
    javaArray
}

private fun MemScope.getLoadedClasses(): Sequence<jclass> = run {
    val count = alloc<jintVar>()
    val classes = alloc<CPointerVar<jclassVar>>()
    GetLoadedClasses(count.ptr, classes.ptr)
    classes.value!!.sequenceOf(count.value)
}

private fun ByteArray.decodePackages(): List<String> = takeIf { it.any() }?.run {
    decodeToString().split(',')
} ?: emptyList()
