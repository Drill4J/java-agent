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
@file:Suppress("unused")
package com.epam.drill.agent.interceptor

import kotlin.native.concurrent.freeze
import kotlinx.cinterop.*
import com.epam.drill.agent.jvmapi.checkEx
import com.epam.drill.agent.jvmapi.env
import com.epam.drill.agent.jvmapi.gen.*
import com.epam.drill.agent.jvmapi.jvmti
import com.epam.drill.agent.jvmapi.vmGlobal
import com.epam.drill.logging.LoggingConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("unused_parameter")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): Int = memScoped {
    vmGlobal.value = vmPointer.freeze()
    val vm = vmPointer.pointed
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    jvmti.value = jvmtiEnvPtr.value.freeze()

    LoggingConfiguration.readDefaultConfiguration()
    LoggingConfiguration.setLoggingLevels("TRACE")
    LoggingConfiguration.setLogMessageLimit(524288)

    val alloc = alloc<jvmtiEventCallbacks>()
    alloc.VMInit = staticCFunction(::vmInitEvent)
    SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)

    JNI_OK
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("unused_parameter")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    HttpInterceptorConfigurer(TestHeadersRetriever, TestRequestHolder)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String) = checkEx(errCode, funName)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("currentEnvs")
fun currentEnvs() = env

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("jvmtii")
fun jvmtii() = jvmti.value

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("getJvm")
fun getJvm() = vmGlobal.value
