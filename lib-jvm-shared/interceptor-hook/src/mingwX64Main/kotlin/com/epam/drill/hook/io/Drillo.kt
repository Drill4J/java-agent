/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("ObjectPropertyName")

package com.epam.drill.hook.io


import com.epam.drill.hook.gen.*
import kotlinx.atomicfu.*
import kotlinx.cinterop.*
import kotlinx.collections.immutable.*
import platform.posix.*
import platform.windows.LPDWORD
import platform.windows._OVERLAPPED

@OptIn(ExperimentalForeignApi::class)
actual val tcpInitializer = run {
    val socketHook = funchook_create()
    funchook_prepare(socketHook, close_func_point, staticCFunction(::drillClose)).check("prepare close_func_point")
    funchook_prepare(
        socketHook,
        connect_func_point,
        staticCFunction(::drillConnect)
    ).check("prepare connect_func_point")
    funchook_prepare(socketHook, accept_func_point, staticCFunction(::drillAccept)).check("prepare accept_func_point")
    funchook_install(socketHook, 0).check("funchook_install")
}

@OptIn(ExperimentalForeignApi::class)
actual fun drillRead(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()
    val read = nativeRead(fd.convert(), buf, size.convert())
    if (read > 0 && (accepts.contains(fd.convert()) || connects.contains(fd.convert())))
        tryDetectProtocol(fd.convert(), buf, read.convert())
    return read.convert()
}

@OptIn(ExperimentalForeignApi::class)
actual fun drillWrite(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()

    return memScoped {
        val (finalBuf, finalSize, ll) =
            if (accepts.contains(fd.convert()) || connects.contains(fd.convert()))
                processWriteEvent(fd.convert(), buf, size.convert())
            else TcpFinalData(buf, size.convert())
        (nativeWrite(fd.convert(), finalBuf, (finalSize).convert()) - ll).convert()
    }
}


@OptIn(ExperimentalForeignApi::class)
actual fun drillSend(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    return memScoped {
        val (finalBuf, finalSize, ll) = processWriteEvent(fd, buf, size.convert())
        (nativeSend(fd.convert(), finalBuf, (finalSize).convert(), flags) - ll).convert()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun drillRecv(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    val read = nativeRecv(fd, buf, size.convert(), flags)
    tryDetectProtocol(fd, buf, read.convert())
    return read.convert()
}

@OptIn(ExperimentalForeignApi::class)
fun drillAccept(
    fd: DRILL_SOCKET,
    addr: CPointer<sockaddr>?,
    socklen: CPointer<drill_sock_lenVar>?,
): DRILL_SOCKET {
    initRuntimeIfNeeded()
    val socket = nativeAccept(fd, addr, socklen)
    if (isValidSocket(socket) == 0)
        _accepts.update { it + socket }
    return socket
}

@OptIn(ExperimentalForeignApi::class)
actual fun configureTcpHooks() {
    configureTcpHooksBuild {
        println("Configuration for mingw")
        funchook_prepare(tcpHook, wsaSend_func_point, staticCFunction(::drillWsaSend))
        funchook_prepare(tcpHook, wsaRecv_func_point, staticCFunction(::drillWsaRecv))
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun drillWsaSend(
    fd: SOCKET,
    buff: CPointer<_WSABUF>?,
    buffersSize: UInt,
    written: LPDWORD?,
    p5: UInt,
    p6: CPointer<_OVERLAPPED>?,
    p7: LPWSAOVERLAPPED_COMPLETION_ROUTINE?,
): Int {
    initRuntimeIfNeeded()
    val buffer = buff!![0]
    val size = buffer.len
    val buf = buffer.buf
    return memScoped {
        val (finalBuf, finalSize, injectedSize) = processWriteEvent(fd.convert(), buf, size.convert())
        buff[0].buf = finalBuf
        buff[0].len = finalSize.convert()
        val wsasendFunc = wsaSend_func!!(fd, buff, buffersSize, written, p5, p6, p7)
        written!!.pointed.value -= injectedSize.convert()
        (wsasendFunc).convert()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun drillWsaRecv(
    fd: SOCKET,
    buff: CPointer<_WSABUF>?,
    p3: UInt,
    read: LPDWORD?,
    p5: LPDWORD?,
    p6: CPointer<_OVERLAPPED>?,
    p7: LPWSAOVERLAPPED_COMPLETION_ROUTINE?,
): Int {
    initRuntimeIfNeeded()
    val wsarecvFunc: Int = wsaRecv_func!!(fd, buff, p3, read, p5, p6, p7)
    val finalBuf = buff!!.pointed
    tryDetectProtocol(fd, finalBuf.buf, read!!.pointed.value.convert())
    return wsarecvFunc
}
