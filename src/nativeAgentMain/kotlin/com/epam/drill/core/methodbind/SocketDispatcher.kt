@file:Suppress("SpellCheckingInspection")

package com.epam.drill.core.methodbind

import com.epam.drill.core.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
import kotlin.math.*

const val SocketDispatcher = "Lsun/nio/ch/SocketDispatcher;"
const val SocketOutputStream = "Ljava/net/SocketOutputStream;"
const val FileDispatcherImpl = "Lsun/nio/ch/FileDispatcherImpl;"
const val Netty = "Lio/netty/channel/unix/FileDescriptor;"
const val CR: Byte = 13
const val LF: Byte = 10

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
        println("socket parse exception: ${ex.message}")
    }
}

fun fillRequestToHolder(@Suppress("UNUSED_PARAMETER") request: String) {
    val requestPattern = exec { requestPattern }
    val (requestHolderClass, requestHolder: jobject?) = instance("com/epam/drill/ws/RequestHolder")
    val retrieveClassesData = GetMethodID(requestHolderClass, "storeRequest", "(Ljava/lang/String;Ljava/lang/String;)V")
    CallVoidMethod(requestHolder, retrieveClassesData, NewStringUTF(request), NewStringUTF(requestPattern))
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

fun socketWrite0(
    env: CPointer<JNIEnvVar>,
    clazz: jobject,
    fd: jobject,
    address: jbyteArray,
    off: jint,
    len: jint
): jint {
    initRuntimeIfNeeded()

    val prefix = GetByteArrayElements(address, null)!!.readBytes(min(off + 4, len)).decodeToString()
    if (isAllowedVerb(prefix)) {
        val spyHeaders = generateHeaders()
        val contentBytes = GetByteArrayElements(address, null)!!.readBytes(len)
        val headersDelimiterIndex = contentBytes.indexOf(byteArrayOf(CR, LF, CR, LF))
        val headersBytes = contentBytes.take(headersDelimiterIndex)
        val headersDecoded = headersBytes.toByteArray().decodeToString()

        if (headersDelimiterIndex > 0 && isSutableContentType(headersDecoded) && !headersDecoded.contains("drill-session-id")) {
            val contentBody = contentBytes.copyOfRange(headersDelimiterIndex, len)
            val modifiedHeaders = injectHeaders(headersDecoded, spyHeaders)
            val modifiedContent = modifiedHeaders.plus(contentBody)
            val modifiedContentLength = modifiedContent.size
            val newByteArray: jbyteArray? = NewByteArray(modifiedContentLength)
            SetByteArrayRegion(
                newByteArray, 0, modifiedContentLength,
                getBytes(newByteArray, modifiedContent)
            )
            exec { originalMethod[::socketWrite0] }(env, clazz, fd, newByteArray!!, off, modifiedContentLength)
        } else {
            exec { originalMethod[::socketWrite0] }(env, clazz, fd, address, off, len)
        }
    } else {
        exec { originalMethod[::socketWrite0] }(env, clazz, fd, address, off, len)
    }
    return len
}

private fun getBytes(
    newByteArray: jbyteArray?,
    classData: ByteArray
): CPointer<jbyteVar>? {
    val bytess: CPointer<jbyteVar>? = GetByteArrayElements(newByteArray, null)
    classData.forEachIndexed { index, byte ->
        bytess!![index] = byte
    }
    return bytess
}

fun write(
    address: DirectBufferAddress, len: jint,
    block: (DirectBufferAddress, jint) -> jint
): Int {
    initRuntimeIfNeeded()

    val prefix = address.rawString(min(4, len))

    if (isAllowedVerb(prefix)) {
        val spyHeaders = generateHeaders()
        val contentBytes = address.toPointer().readBytes(len)
        val headersDelimiterIndex = contentBytes.indexOf(byteArrayOf(CR, LF, CR, LF))
        val headersBytes = contentBytes.take(headersDelimiterIndex)
        val headersDecoded = headersBytes.toByteArray().decodeToString()

        if (headersDelimiterIndex > 0 && isSutableContentType(headersDecoded) && !headersDecoded.contains("drill-session-id")) {
            val headersLength = headersBytes.size
            val contentBody = contentBytes.copyOfRange(headersLength, len)
            val scope = Arena()
            val modifiedHeaders = injectHeaders(headersDecoded, spyHeaders)
            val modifiedContent = modifiedHeaders.plus(contentBody)
            val refToModifiedContent = modifiedContent.refTo(0)
            val bufferAddress: DirectBufferAddress = refToModifiedContent.getPointer(scope).toLong()
            block(bufferAddress, modifiedContent.size)
            scope.clear()
        } else {
            block(address, len)
        }
    } else {
        block(address, len)
    }
    return len
}

fun ByteArray.indexOf(arr: ByteArray): Int {

    for (index in indices) {

        if (index + arr.size <= size) {
            val regionMatches = arr.foldIndexed(true) { i, acc, byte ->
                acc && this[index + i] == byte
            }

            if (regionMatches) return index
        } else break
    }
    return -1
}

private fun injectHeaders(contentBodyBytes: String, spyHeaders: String): ByteArray {
    val injectHeaders = contentBodyBytes.replaceFirst("\n", "$spyHeaders\n")
    return injectHeaders.toUtf8Bytes()
}

private fun isAllowedVerb(prefix: String) = prefix == "HTTP" || prefix == "POST" || prefix == "GET "

private fun isSutableContentType(contentBodyBytes: String): Boolean {
    return (contentBodyBytes.contains("text/html")
            || contentBodyBytes.contains("application/json")
            || contentBodyBytes.contains("text/plain"))
}

private fun generateHeaders(): String {
    val sessionId = sessionId()
    val spyHeaders = exec {
        val adminUrl = if (::secureAdminAddress.isInitialized) {
            secureAdminAddress.toUrlString(false)
        } else adminAddress.toUrlString(false)
        "\n" +
                "drill-agent-id: ${if (agentConfig.serviceGroupId.isEmpty()) agentConfig.id else agentConfig.serviceGroupId}\n" +
                "drill-admin-url: $adminUrl" +
                if (sessionId != null) "\ndrill-session-id: $sessionId" else ""
    }
    return spyHeaders
}
