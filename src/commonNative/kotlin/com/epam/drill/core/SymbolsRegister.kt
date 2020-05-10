@file:Suppress("unused", "FunctionName")

package com.epam.drill.core

import com.epam.drill.agent.*
import com.epam.drill.agent.jvmapi.*
import com.epam.drill.api.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import mu.*
import kotlin.native.concurrent.*
import kotlin.time.*

@SharedImmutable
private val logger = KotlinLogging.logger("SymbolsRegister")

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
    val allPrefixes = (state.packagePrefixes + decodedPrefixes).distinct().map { "L$it" }
    logger.info { "Package prefixes: $allPrefixes." }
    if (allPrefixes.any()) {
        getLoadedClasses()
            .filter { jclass ->
                val classSignature = jclass.signature()
                '$' !in classSignature && allPrefixes.any { classSignature.startsWith(it) }
            }
            .filter { it.isValid() }
            .toList().apply {
                logger.info { "$size classes to retransform." }
                val duration = measureTime { RetransformClasses(size, toCValues()) }
                logger.info { "Retransformed $size classes in $duration." }
            }.size
    } else 0
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
