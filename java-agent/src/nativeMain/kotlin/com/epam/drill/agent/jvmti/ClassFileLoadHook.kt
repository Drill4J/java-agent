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

import kotlin.native.concurrent.AtomicInt
import kotlinx.cinterop.*
import org.objectweb.asm.ClassReader
import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.bits.loadByteArray
import io.ktor.utils.io.bits.of
import mu.KotlinLogging
import com.epam.drill.agent.CADENCE_CONSUMER
import com.epam.drill.agent.CADENCE_PRODUCER
import com.epam.drill.agent.DRILL_PACKAGE
import com.epam.drill.agent.JvmModuleStorage
import com.epam.drill.agent.KAFKA_CONSUMER_SPRING
import com.epam.drill.agent.KAFKA_PRODUCER_INTERFACE
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.instrument.CadenceTransformer
import com.epam.drill.agent.instrument.KafkaTransformer
import com.epam.drill.agent.instrument.NettyTransformer
import com.epam.drill.agent.instrument.SSLTransformer
import com.epam.drill.agent.instrument.TTLTransformer
import com.epam.drill.agent.instrument.TomcatTransformer
import com.epam.drill.agent.module.InstrumentationAgentModule
import com.epam.drill.common.classloading.ClassSource
import com.epam.drill.instrument.http.ApacheClient
import com.epam.drill.instrument.http.JavaHttpUrlConnection
import com.epam.drill.instrument.http.OkHttpClient
import com.epam.drill.jvmapi.gen.Allocate
import com.epam.drill.jvmapi.gen.jclass
import com.epam.drill.jvmapi.gen.jint
import com.epam.drill.jvmapi.gen.jintVar
import com.epam.drill.jvmapi.gen.jobject

object ClassFileLoadHook {

    private val logger = KotlinLogging.logger("com.epam.drill.agent.jvmti.ClassFileLoadHook")

    private val strategies = listOf(JavaHttpUrlConnection, ApacheClient, OkHttpClient)

    private val isAsyncApp = Configuration.parameters[ParameterDefinitions.IS_ASYNC_APP]
    private val isTlsApp = Configuration.parameters[ParameterDefinitions.IS_TLS_APP]
    private val isWebApp = Configuration.parameters[ParameterDefinitions.IS_WEB_APP]
    private val isKafka = Configuration.parameters[ParameterDefinitions.IS_KAFKA]
    private val isCadence = Configuration.parameters[ParameterDefinitions.IS_CADENCE]

    private val totalTransformClass = AtomicInt(0)

    operator fun invoke(
        classBeingRedefined: jclass?,
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
        if (isBootstrapClassLoading(loader, protectionDomain) && !isTlsApp && !isAsyncApp
            && !kClassName.contains("Http") // raw hack for http(s) classes
        ) return
        if (classData == null || kClassName.startsWith(DRILL_PACKAGE)) return
        try {
            val classBytes = ByteArray(classDataLen).apply {
                Memory.of(classData, classDataLen).loadByteArray(0, this)
            }
            val transformers = mutableListOf<(ByteArray) -> ByteArray?>()
            val classReader = ClassReader(classBytes)
            val superName = classReader.superName ?: ""
            val interfaces = classReader.interfaces.filterNotNull()
            //TODO needs refactoring EPMDJ-8528
            if (isAsyncApp || isWebApp) {
                if (isAsyncApp && isTTLCandidate(kClassName, superName, interfaces)) {
                    transformers += { bytes ->
                        TTLTransformer.transform(
                            loader,
                            kClassName,
                            classBeingRedefined,
                            bytes
                        )
                    }
                }
                if (superName == SSLTransformer.SSL_ENGINE_CLASS_NAME) {
                    transformers += { bytes -> SSLTransformer.transform(kClassName, bytes, loader, protectionDomain) }
                }
            }

            if (isKafka) {
                if (KAFKA_PRODUCER_INTERFACE in interfaces) {
                    transformers += { bytes ->
                        KafkaTransformer.transform(KAFKA_PRODUCER_INTERFACE, bytes, loader, protectionDomain)
                    }
                }
                if (kClassName == KAFKA_CONSUMER_SPRING) {
                    transformers += { bytes ->
                        KafkaTransformer.transform(KAFKA_CONSUMER_SPRING, bytes, loader, protectionDomain)
                    }
                }
            }
            if (isCadence) {
                if (CADENCE_PRODUCER == kClassName || CADENCE_CONSUMER == kClassName) {
                    transformers += { bytes -> CadenceTransformer.transform(kClassName, bytes, loader, protectionDomain ) }
                }
            }
            val classSource = ClassSource(kClassName, classReader.superName ?: "", classBytes)
            if ('$' !in kClassName && classSource.prefixMatches(Configuration.agentMetadata.packagesPrefixes)) {
                JvmModuleStorage.values().filterIsInstance<InstrumentationAgentModule>().forEach { plugin ->
                    transformers += { bytes -> plugin.instrument(kClassName, bytes) }
                }
            }
            if (kClassName.startsWith("org/apache/catalina/core/ApplicationFilterChain")) {
                transformers += { bytes ->
                    TomcatTransformer.transform(kClassName, bytes, loader, protectionDomain)
                }
            }

            strategies.forEach { strategy ->
                if (strategy.permit(classReader.className, classReader.superName, classReader.interfaces)) {
                    transformers += { strategy.transform(kClassName, classBytes, loader, protectionDomain) }
                }
            }
            // TODO Http hook does not work for Netty on linux system
            if ('$' !in kClassName && kClassName.startsWith(NettyTransformer.HANDLER_CONTEXT)) {
                logger.info { "Starting transform Netty class kClassName $kClassName..." }
                transformers += { bytes ->
                    NettyTransformer.transform(kClassName, bytes, loader, protectionDomain)
                }
            }
            if (transformers.any()) {
                transformers.fold(classBytes) { bytes, transformer ->
                    transformer(bytes) ?: bytes
                }.takeIf { it !== classBytes }?.let { newBytes ->
                    logger.trace { "$kClassName transformed" }
                    totalTransformClass.addAndGet(1).takeIf { it % 100 == 0 }?.let {
                        logger.debug { "$it classes are transformed" }
                    }
                    convertToNativePointers(newBytes, newData, newClassDataLen)
                }
            }
        } catch (throwable: Throwable) {
            logger.error(throwable) {
                "Can't retransform class: $kClassName, ${classData.readBytes(classDataLen).contentToString()}"
            }
        }
    }

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

    private fun isBootstrapClassLoading(loader: jobject?, protectionDomain: jobject?) =
        loader == null || protectionDomain == null

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
