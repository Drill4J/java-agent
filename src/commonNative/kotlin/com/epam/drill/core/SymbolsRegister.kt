@file:Suppress("unused", "FunctionName")

package com.epam.drill.core

import com.epam.drill.agent.jvmapi.*
import com.epam.drill.api.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*

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
    val packagesPrefixes =
        prefixes.readBytes()?.let { Cbor.load(String.serializer().set, it).map { "L$it;" } } ?: return@memScoped 0
    getLoadedClasses()
        .associateBy { alloc<CPointerVar<ByteVar>>().apply { GetClassSignature(it, ptr, null) }.value!! }
        .filterKeys { packagesPrefixes.contains(it.toKString()) }.values.toList()
        .apply { RetransformClasses(size, toCValues()) }
        .size
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

private fun MemScope.getLoadedClasses() = run {
    val count = alloc<jintVar>()
    val classes = alloc<CPointerVar<jclassVar>>()
    GetLoadedClasses(count.ptr, classes.ptr)
    classes.value!!.sequenceOf(count.value)
}
