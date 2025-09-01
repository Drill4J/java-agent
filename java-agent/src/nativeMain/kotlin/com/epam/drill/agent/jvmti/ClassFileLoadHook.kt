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
package com.epam.drill.agent.jvmti

import com.epam.drill.agent.instrument.clients.ApacheHttpClientTransformer
import com.epam.drill.agent.instrument.clients.JavaHttpClientTransformer
import com.epam.drill.agent.instrument.clients.OkHttp3ClientTransformer
import com.epam.drill.agent.instrument.clients.SpringWebClientTransformer
import com.epam.drill.agent.instrument.servers.ReactorTransformer
import com.epam.drill.agent.instrument.servers.*
import com.epam.drill.agent.instrument.jetty.*
import com.epam.drill.agent.instrument.netty.*
import com.epam.drill.agent.instrument.tomcat.*
import com.epam.drill.agent.instrument.undertow.*
import com.epam.drill.agent.instrument.ApplicationClassTransformer
import com.epam.drill.agent.jvmapi.gen.Allocate
import com.epam.drill.agent.jvmapi.gen.jint
import com.epam.drill.agent.jvmapi.gen.jintVar
import com.epam.drill.agent.jvmapi.gen.jobject
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import mu.KotlinLogging
import org.objectweb.asm.ClassReader
import kotlin.concurrent.AtomicInt
import kotlinx.cinterop.ExperimentalForeignApi

object ClassFileLoadHook {

    private const val DRILL_PACKAGE = "com/epam/drill/agent"

    private val logger = KotlinLogging.logger("com.epam.drill.agent.jvmti.ClassFileLoadHook")

    private val allTransformers = listOf(
        ApplicationClassTransformer,
        TomcatHttpServerTransformer,
        JettyHttpServerTransformer,
        UndertowHttpServerTransformer,
        NettyHttpServerTransformer,
        JavaHttpClientTransformer,
        ApacheHttpClientTransformer,
        OkHttp3ClientTransformer,
        SpringWebClientTransformer,
        KafkaTransformer,
        CadenceTransformer,
        TTLTransformer,
        ReactorTransformer,
        SSLEngineTransformer,
        JettyWsClientTransformer,
        JettyWsServerTransformer,
        Jetty9WsMessagesTransformer,
        Jetty10WsMessagesTransformer,
        Jetty11WsMessagesTransformer,
        NettyWsClientTransformer,
        NettyWsServerTransformer,
        NettyWsMessagesTransformer,
        TomcatWsClientTransformer,
        TomcatWsServerTransformer,
        TomcatWsMessagesTransformer,
        UndertowWsClientTransformer,
        UndertowWsServerTransformer,
        UndertowWsMessagesTransformer,
        CompatibilityTestsTransformer,
    )


    private val totalTransformClass = AtomicInt(0)

    @OptIn(ExperimentalForeignApi::class)
    operator fun invoke(
        loader: jobject?,
        clsName: CPointer<ByteVar>?,
        protectionDomain: jobject?,
        classDataLen: jint,
        classData: CPointer<UByteVar>?,
        newClassDataLen: CPointer<jintVar>?,
        newData: CPointer<CPointerVar<UByteVar>>?,
    ) {
        initRuntimeIfNeeded()
        val kClassName = clsName?.toKString() ?: return
        val kClassData = classData ?: return

        val precheckedTransformers = allTransformers
            .filter { it.enabled() }
            .filterNot { kClassName.startsWith(DRILL_PACKAGE) }
            .filter { it.precheck(kClassName, loader, protectionDomain) }
            .takeIf { it.any() }
            ?: return

        val (oldClassBytes, reader) = runCatching {
            val classBytes = ByteArray(classDataLen).apply {
                Memory.of(kClassData, classDataLen).loadByteArray(0, this)
            }
            classBytes to ClassReader(classBytes)
        }.onFailure {
            logger.error(it) { "Can't read class: $kClassName" }
        }.getOrNull() ?: return

        val permittedTransformers = precheckedTransformers.filter {
            it.permit(
                kClassName,
                reader.superName,
                reader.interfaces
            )
        }

        val newClassBytes = permittedTransformers.fold(oldClassBytes) { bytes, transformer ->
            runCatching {
                transformer.transform(kClassName, bytes, loader, protectionDomain)
            }.onFailure {
                logger.error(it) { "Can't transform class: $kClassName with ${transformer::class}" }
            }.getOrDefault(bytes)
        }

        if (newClassBytes !== oldClassBytes) {
            logger.debug { "$kClassName transformed" }
            totalTransformClass.addAndGet(1).takeIf { it % 100 == 0 }?.let {
                logger.debug { "At least $it classes were transformed" }
            }
            convertToNativePointers(newClassBytes, newData, newClassDataLen)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun convertToNativePointers(
        instrumentedBytes: ByteArray,
        newData: CPointer<CPointerVar<UByteVar>>?,
        newClassDataLen: CPointer<jintVar>?,
    ) {
        val instrumentedSize = instrumentedBytes.size
        Allocate(instrumentedSize.toLong(), newData)
        instrumentedBytes.forEachIndexed { index, byte ->
            val innerValue = newData!!.pointed.value!!
            innerValue[index] = byte.toUByte()
        }
        newClassDataLen!!.pointed.value = instrumentedSize
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isBootstrapClassLoading(loader: jobject?, protectionDomain: jobject?) =
        loader == null || protectionDomain == null

    @OptIn(ExperimentalForeignApi::class)
    private fun isTTLCandidate(
        kClassName: String,
        superName: String,
        interfaces: Collection<String>,
    ) = kClassName in TTLTransformer.directTtlClasses
            || (kClassName != TTLTransformer.timerTaskClass
            && (TTLTransformer.runnableInterface in interfaces
            || superName == TTLTransformer.poolExecutor))
            && !kClassName.startsWith(TTLTransformer.jdkInternal)

}
