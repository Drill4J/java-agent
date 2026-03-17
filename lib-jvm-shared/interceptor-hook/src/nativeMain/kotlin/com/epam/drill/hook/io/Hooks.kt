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
package com.epam.drill.hook.io

import com.epam.drill.hook.gen.DRILL_SOCKET
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlin.native.concurrent.freeze


@SharedImmutable
private val _interceptors = atomic(persistentListOf<Interceptor>())

val interceptors: List<Interceptor>
    get() = _interceptors.value

fun addInterceptor(interceptor: Interceptor) {
    _interceptors.update { it + interceptor }
}

interface ReadInterceptor {
    @OptIn(ExperimentalForeignApi::class)
    fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int)
}

interface WriteInterceptor {
    @OptIn(ExperimentalForeignApi::class)
    fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData
}

interface Interceptor : ReadInterceptor, WriteInterceptor {
    @OptIn(ExperimentalForeignApi::class)
    fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>): Boolean
    @OptIn(ExperimentalForeignApi::class)
    fun close(fd: DRILL_SOCKET)
}


@OptIn(ExperimentalForeignApi::class)
fun tryDetectProtocol(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int) {
    buf?.let { byteBuf ->
        interceptors.forEach {
            it.let {
                if (it.isSuitableByteStream(fd, byteBuf)) {
                    memScoped {
                        with(it) {
                            interceptRead(fd, buf, size)
                        }
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun close(fd: DRILL_SOCKET) {
    interceptors.forEach {
        it.close(fd)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun MemScope.processWriteEvent(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int): TcpFinalData {
    return buf?.let { byteBuf ->
        interceptors.forEach {
            it.let {
                if (it.isSuitableByteStream(fd, byteBuf))
                    return with(it) {
                        interceptWrite(fd, buf, size)
                    }
                else TcpFinalData(buf, size)
            }
        }
        TcpFinalData(buf, size)
    } ?: TcpFinalData(buf, size)


}

@SharedImmutable
val CR_LF = "\r\n"

@SharedImmutable
val CR_LF_BYTES = CR_LF.encodeToByteArray()

@SharedImmutable
val HEADERS_DELIMITER = CR_LF_BYTES + CR_LF_BYTES

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val injectedHeaders = atomic({ emptyMap<String, String>() }.freeze()).freeze()

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val readHeaders = atomic({ _: Map<ByteArray, ByteArray> -> }.freeze()).freeze()

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val readCallback = atomic({ _: ByteArray -> }.freeze()).freeze()

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val writeCallback = atomic({ _: ByteArray -> }.freeze()).freeze()