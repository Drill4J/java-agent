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
@file:Suppress("unused", "FunctionName")

package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.api.*
import com.epam.drill.common.classloading.ClassSource
import com.epam.drill.common.classloading.SUBCLASS_OF
import com.epam.drill.core.callbacks.classloading.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.*
import kotlin.time.*
import mu.KotlinLogging

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.core.SymbolsRegister")

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_PluginSender_send")
fun sendFromJava(envs: JNIEnv, thiz: jobject, jpluginId: jstring, jmessage: jstring) = withJString {
    sendToSocket(jpluginId.toKString(), jmessage.toKString())
}

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_epam_drill_plugin_api_Native_RetransformClassesByPackagePrefixes")
fun RetransformClassesByPackagePrefixes(env: JNIEnv, thiz: jobject, prefixes: jbyteArray): jint = memScoped {
    val decodedPrefixes = prefixes.readBytes()?.decodePackages() ?: emptyList()
    val allPrefixes = (state.packagePrefixes + decodedPrefixes).filter {
        !it.startsWith(DRILL_PACKAGE)
    }.distinct()
    logger.info { "Package prefixes: $allPrefixes." }
    totalTransformClass.value = 0
    allPrefixes.takeIf { it.any() }?.let { prefixes ->
        getLoadedClasses().filter {
            it.status() in 0.toUInt()..7.toUInt()
        }.filter { jclass ->
            val signature = jclass.signature()
            val superclass = jni.takeIf { prefixes.any { it.startsWith(SUBCLASS_OF) } }
                ?.GetSuperclass
                ?.invoke(com.epam.drill.jvmapi.env, jclass)
            val classSource = ClassSource(signature, superclass?.signature() ?: "")
            '$' !in signature && classSource.prefixMatches(prefixes, 1)
        }.partition { it.status() == 7.toUInt() }.let { (loaded, undetermined) ->
            logger.info { "${loaded.size + undetermined.size} classes to retransform." }
            measureTimedValue {
                val processedCount: Int = measureTimedValue {
                    RetransformClasses(loaded.size, loaded.toCValues())
                }.let { (retCode, duration) ->
                    loaded.size.takeIf { retCode == 0.toUInt() }?.also {
                        logger.info { "Retransformed ${loaded.size} loaded classes in $duration." }
                    } ?: 0.also { logger.error { "Error retransforming loaded classes." } }
                }
                val failed: List<jclass> = loaded.takeIf { processedCount == 0 } ?: emptyList()
                (failed + undetermined).chunked(32) { chunk ->
                    if (RetransformClasses(chunk.size, chunk.toCValues()) != 0.toUInt()) {
                        logger.error { "Error retransforming chunk, starting per-class retransforming for the chunk." }
                        chunk.count { jclass ->
                            (RetransformClasses(1, cValuesOf(jclass)) == 0.toUInt()).also {
                                if (!it) logger.error {
                                    "Error retransforming ${jclass.signature()}, status=${jclass.status()}"
                                }
                            }
                        }
                    } else chunk.size
                }.sum().plus(processedCount)
            }.also {
                logger.info { "Retransformed ${it.value} classes in ${it.duration}." }
            }.value
        }
    } ?: 0
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

@CName("Java_com_epam_drill_plugin_api_Native_GetPackagePrefixes")
fun GetPackagePrefixes(): jstring? {
    val packagesPrefixes = agentConfig.packagesPrefixes.packagesPrefixes
    return NewStringUTF(packagesPrefixes.joinToString(", "))
}

@CName("Java_com_epam_drill_plugin_api_Native_GetScanClassPath")
fun GetScanClassPath(): jstring? {
    return NewStringUTF(config.scanClassPath)
}

@CName("Java_com_epam_drill_plugin_api_Native_WaitClassScanning")
fun WaitClassScanning() = runBlocking {
    val classScanDelay = config.classScanDelay - state.startMark.elapsedNow()
    if (classScanDelay.isPositive()) {
        logger.debug { "Waiting class scan delay ($classScanDelay left)..." }
        delay(classScanDelay)
    }
}

private fun MemScope.getLoadedClasses(): Sequence<jclass> = run {
    val count = alloc<jintVar>()
    val classes = alloc<CPointerVar<jclassVar>>()
    GetLoadedClasses(count.ptr, classes.ptr)
    classes.value!!.sequenceOf(count.value)
}

private fun ByteArray.decodePackages(): List<String> = takeIf { it.any() }?.run {
    decodeToString().split(',')
} ?: emptyList()
