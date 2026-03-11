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
actual fun configureTcpHooks() = configureTcpHooksBuild {
    println("Configuration for unix")
    funchook_prepare(tcpHook, writev_func_point, staticCFunction(::writevDrill))
    funchook_prepare(tcpHook, readv_func_point, staticCFunction(::readvDrill))
}

@OptIn(ExperimentalForeignApi::class)
private fun readvDrill(fd: Int, iovec: CPointer<iovec>?, size: Int): ssize_t {
    val convert = readv_func!!(fd, iovec, size).convert<ssize_t>()
    println("readv intercepted do not implemented now. If you see this message, please put issue to https://github.com/Drill4J/Drill4J")
    return convert
}


@OptIn(ExperimentalForeignApi::class)
private fun writevDrill(fd: Int, iovec: CPointer<iovec>?, size: Int): ssize_t {
    return memScoped {
        //todo I think we(headers) should be in the first buffer
        val iovecs = iovec!![0]
        val iovLen = iovecs.iov_len
        val base = iovecs.iov_base!!.reinterpret<ByteVarOf<Byte>>()
        val (finalBuf, finalSize, injectedSize) = processWriteEvent(fd.convert(), base, iovLen.convert())
        iovec[0].iov_base = finalBuf
        iovec[0].iov_len = finalSize.convert()
        (writev_func!!(fd, iovec, size) - injectedSize).convert()
    }
}
