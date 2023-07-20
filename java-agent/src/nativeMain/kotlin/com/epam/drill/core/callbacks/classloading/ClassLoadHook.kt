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
@file:Suppress("UNUSED_VARIABLE")

package com.epam.drill.core.callbacks.classloading

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.SSLTransformer.SSL_ENGINE_CLASS_NAME
import com.epam.drill.agent.instrument.http.apache.*
import com.epam.drill.agent.instrument.http.java.*
import com.epam.drill.agent.instrument.http.ok.*
import com.epam.drill.common.classloading.ClassSource
import com.epam.drill.core.Agent.isHttpHookEnabled
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.request.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import org.objectweb.asm.*
import kotlin.native.concurrent.*
import mu.KotlinLogging

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.core.callbacks.classloading.ClassLoadHook")

@SharedImmutable
private val strategys = listOf(JavaHttpUrlConnection, ApacheClient, OkHttpClient)

internal val totalTransformClass = AtomicInt(0)

@Suppress("unused", "UNUSED_PARAMETER")
fun classLoadEvent(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    clsName: CPointer<ByteVar>?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?,
) {
    initRuntimeIfNeeded()
    val kClassName = clsName?.toKString() ?: return
    if (isBootstrapClassLoading(loader, protection_domain) && !config.isTlsApp && !config.isAsyncApp
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
        if (config.isAsyncApp || config.isWebApp) {
            if (config.isAsyncApp && isTTLCandidate(kClassName, superName, interfaces)) {
                transformers += { bytes ->
                    TTLTransformer.transform(
                        loader,
                        kClassName,
                        classBeingRedefined,
                        bytes
                    )
                }
            }
            if (superName == SSL_ENGINE_CLASS_NAME) {
                transformers += { bytes -> SSLTransformer.transform(kClassName, bytes, loader, protection_domain) }
            }
        }

        if (config.isKafka) {
            if (KAFKA_PRODUCER_INTERFACE in interfaces) {
                transformers += { bytes ->
                    KafkaTransformer.transform(KAFKA_PRODUCER_INTERFACE, bytes, loader, protection_domain)
                }
            }
            if (kClassName == KAFKA_CONSUMER_SPRING) {
                transformers += { bytes ->
                    KafkaTransformer.transform(KAFKA_CONSUMER_SPRING, bytes, loader, protection_domain)
                }
            }
        }
        if (config.isCadence) {
            if (CADENCE_PRODUCER == kClassName || CADENCE_CONSUMER == kClassName) {
                transformers += { bytes -> CadenceTransformer.transform(kClassName, bytes, loader, protection_domain) }
            }
        }
        val classSource = ClassSource(kClassName, classReader.superName ?: "", classBytes)
        if ('$' !in kClassName && classSource.prefixMatches(state.packagePrefixes)) {
            pstorage.values.filterIsInstance<InstrumentationNativePlugin>().forEach { plugin ->
                transformers += { bytes -> plugin.instrument(kClassName, bytes) }
            }
        }
        if (!isHttpHookEnabled) {
            if (kClassName.startsWith("org/apache/catalina/core/ApplicationFilterChain")) {
                logger.info { "Http hook is off, starting transform tomcat class kClassName $kClassName..." }
                transformers += { bytes ->
                    TomcatTransformer.transform(kClassName, bytes, loader, protection_domain)
                }
            }

            strategys.forEach { strategy ->
                if (strategy.permit(classReader.className, classReader.superName, classReader.interfaces)) {
                    transformers += { strategy.transform(kClassName, classBytes, loader, protection_domain) }
                }
            }
        }
        // TODO Http hook does not work for Netty on linux system
        if ('$' !in kClassName && kClassName.startsWith(NettyTransformer.HANDLER_CONTEXT)) {
            logger.info { "Starting transform Netty class kClassName $kClassName..." }
            transformers += { bytes ->
                NettyTransformer.transform(kClassName, bytes, loader, protection_domain)
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

private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

private fun isTTLCandidate(
    kClassName: String,
    superName: String,
    interfaces: Collection<String>,
) = kClassName in TTLTransformer.directTtlClasses
        || (kClassName != TTLTransformer.timerTaskClass
        && (TTLTransformer.runnableInterface in interfaces
        || superName == TTLTransformer.poolExecutor))
        && !kClassName.startsWith(TTLTransformer.jdkInternal)
