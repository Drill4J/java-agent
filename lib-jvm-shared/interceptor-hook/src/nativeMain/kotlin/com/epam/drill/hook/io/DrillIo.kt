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
import kotlin.native.SharedImmutable
import kotlin.native.ThreadLocal
import kotlin.native.concurrent.*

@Suppress("unused")
@SharedImmutable
expect val tcpInitializer: Unit

@SharedImmutable
private val accessThread = Worker.start(true)

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val _connects = atomic(persistentHashSetOf<DRILL_SOCKET>())

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val _accepts = atomic(persistentHashSetOf<DRILL_SOCKET>())

@OptIn(ExperimentalForeignApi::class)
val connects
    get() = _connects.value

@OptIn(ExperimentalForeignApi::class)
val accepts
    get() = _accepts.value

@OptIn(ExperimentalForeignApi::class)
@ThreadLocal
private var _tcpHook: CPointer<funchook_t>? = null

@OptIn(ExperimentalForeignApi::class, ObsoleteWorkersApi::class)
var tcpHook
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _tcpHook }.result
    set(value) = accessThread.execute(TransferMode.UNSAFE, { value }) { _tcpHook = it }.result

expect fun configureTcpHooks()

@OptIn(ExperimentalForeignApi::class)
fun configureTcpHooksBuild(block: () -> Unit) = if (tcpHook != null) {
    funchook_install(tcpHook, 0).check("funchook_install")
} else {
    tcpHook = funchook_create() ?: run {
        println("Failed to create hook")
        return
    }
    funchook_prepare(tcpHook, read_func_point, staticCFunction(::drillRead)).check("prepare read_func_point")
    funchook_prepare(tcpHook, write_func_point, staticCFunction(::drillWrite)).check("prepare write_func_point")
    funchook_prepare(tcpHook, send_func_point, staticCFunction(::drillSend)).check("prepare send_func_point")
    funchook_prepare(tcpHook, recv_func_point, staticCFunction(::drillRecv)).check("prepare recv_func_point")
    block()
    funchook_install(tcpHook, 0).check("funchook_install")
}


@OptIn(ExperimentalForeignApi::class)
fun removeTcpHook() {
    funchook_uninstall(tcpHook, 0).check("funchook_uninstall")
}

//TODO EPMDJ-8696 Move back to common module

@OptIn(ExperimentalForeignApi::class)
expect fun drillRead(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t

@OptIn(ExperimentalForeignApi::class)
expect fun drillWrite(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t

@OptIn(ExperimentalForeignApi::class)
expect fun drillSend(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int

@OptIn(ExperimentalForeignApi::class)
expect fun drillRecv(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int

@OptIn(ExperimentalForeignApi::class)
internal fun drillConnect(fd: DRILL_SOCKET, addr: CPointer<sockaddr>?, socklen: drill_sock_len): Int {
    initRuntimeIfNeeded()
    val connectStatus = nativeConnect(fd, addr, socklen).convert<Int>()
    if (0 == connectStatus) _connects.update { it + fd }
    return connectStatus
}

@OptIn(ExperimentalForeignApi::class)
fun drillClose(fd: DRILL_SOCKET): Int {
    initRuntimeIfNeeded()
    val result = nativeClose(fd)
    if (result == 0) {
        _accepts.update { it - fd }
        _connects.update { it - fd }
        com.epam.drill.hook.io.close(fd)
    }
    return result
}
