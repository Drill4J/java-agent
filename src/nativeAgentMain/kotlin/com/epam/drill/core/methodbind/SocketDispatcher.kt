@file:Suppress("SpellCheckingInspection")

package com.epam.drill.core.methodbind

import com.epam.drill.core.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
import kotlin.math.*

const val SocketDispatcher = "Lsun/nio/ch/SocketDispatcher;"
const val FileDispatcherImpl = "Lsun/nio/ch/FileDispatcherImpl;"
const val Netty = "Lio/netty/channel/unix/FileDescriptor;"

fun readAddress(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    fd: Int,
    address: DirectBufferAddress,
    pos: jint,
    limit: jint
): Int {
    initRuntimeIfNeeded()
    val retVal = exec { originalMethod[::readAddress] }(env, clazz, fd, address, pos, limit)
    read(retVal, address)
    return retVal
}

fun read0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int {
    initRuntimeIfNeeded()
    val retVal = exec { originalMethod[::read0] }(env, obj, fd, address, len)
    read(retVal, address)
    return retVal
}

private fun read(retVal: Int, address: DirectBufferAddress) {
    if (retVal > 8) {
        val prefix = address.rawString(min(8, retVal))
        defineHttp1RequestType(prefix, address, retVal)
    }
}

private fun defineHttp1RequestType(prefix: String, address: DirectBufferAddress, retVal: Int) {
    try {
        if (prefix.startsWith("HTTP") ||
            prefix.startsWith("OPTIONS ") ||
            prefix.startsWith("GET ") ||
            prefix.startsWith("HEAD ") ||
            prefix.startsWith("POST ") ||
            prefix.startsWith("PUT ") ||
            prefix.startsWith("PATCH ") ||
            prefix.startsWith("DELETE ") ||
            prefix.startsWith("TRACE ") ||
            prefix.startsWith("CONNECT ")
        ) {
            fillRequestToHolder(address.rawString(retVal))
        }

    } catch (ex: Throwable) {
        println(ex.message)
    }
}

fun fillRequestToHolder(@Suppress("UNUSED_PARAMETER") request: String) {
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val retrieveClassesData = GetMethodID(requestHolderClass, "storeRequest", "(Ljava/lang/String;)V")
    CallVoidMethod(requestHolder, retrieveClassesData, NewStringUTF(request))
}

fun sessionId(): String? {
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val retrieveClassesData = GetMethodID(requestHolderClass, "sessionId", "()Ljava/lang/String;")
    val jRawString = CallObjectMethod(requestHolder, retrieveClassesData) ?: return null
    return jRawString.toKString()
}

private fun instance(name: String): Pair<jclass?, jobject?> {
    val requestHolderClass = FindClass(name)
    val selfMethodId: jfieldID? = GetStaticFieldID(requestHolderClass, "INSTANCE", "L$name;")
    val requestHolder: jobject? = GetStaticObjectField(requestHolderClass, selfMethodId)
    return Pair(requestHolderClass, requestHolder)
}

fun readv0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): Int =
    read0(env, obj, fd, address, len)


fun write0(env: CPointer<JNIEnvVar>, obj: jobject, fd: jobject, address: DirectBufferAddress, len: jint): jint {
    return write(address, len) { fb, finalLen ->
        exec { originalMethod[::write0] }(env, obj, fd, fb, finalLen)
    }
}

fun writeAddress(env: CPointer<JNIEnvVar>, clazz: jclass, fd: jint, address: jlong, pos: jint, len: jint): jint {
    return write(address, len) { fb, finalLen ->
        exec { originalMethod[::writeAddress] }(env, clazz, fd, fb, pos, finalLen)
    }
}

fun write(address: DirectBufferAddress, len: jint, block: (DirectBufferAddress, jint) -> jint): Int {
    initRuntimeIfNeeded()
    val fakeLength: jint
    val fakeBuffer: DirectBufferAddress
    val prefix = address.rawString(min(4, len))
    if (prefix == "HTTP" || prefix == "POST" || prefix == "GET ") {
        val sessionId = sessionId()
        val spyHeaders = exec {
            val adminUrl = if (::secureAdminAddress.isInitialized) {
                secureAdminAddress.toUrlString(false)
            } else adminAddress.toUrlString(false)
            "\n" +
                    "drill-agent-id: ${if (agentConfig.serviceGroupId.isEmpty()) agentConfig.id else agentConfig.serviceGroupId}\n" +
                    "drill-admin-url: $adminUrl\n" +
                    "drill-session-id: ${sessionId ?: "empty"}"
        }
        val contentBodyBytes = address.toPointer().toKStringFromUtf8()
        if (contentBodyBytes.contains("text/html")
            || contentBodyBytes.contains("application/json")
            || contentBodyBytes.contains("text/plain")
        ) {
            val replaceFirst = contentBodyBytes.replaceFirst("\n", "$spyHeaders\n")
            val toUtf8Bytes = replaceFirst.toUtf8Bytes()
            val refTo = toUtf8Bytes.refTo(0)
            val scope = Arena()
            fakeBuffer = refTo.getPointer(scope).toLong()
            val additionalSize = spyHeaders.toUtf8Bytes().size
            fakeLength = len + additionalSize
            block(fakeBuffer, fakeLength)
            scope.clear()
        } else {
            block(address, len)
        }
    } else {
        block(address, len)
    }
    return len
}