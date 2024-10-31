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
package com.epam.drill.agent.jvmti

import kotlin.native.concurrent.freeze
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import com.epam.drill.agent.Agent
import com.epam.drill.agent.jvmapi.checkEx
import com.epam.drill.agent.jvmapi.env
import com.epam.drill.agent.jvmapi.gen.JVMTI_VERSION
import com.epam.drill.agent.jvmapi.gen.JavaVMVar
import com.epam.drill.agent.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.agent.jvmapi.gen.jvmtiError
import com.epam.drill.agent.jvmapi.jvmti
import com.epam.drill.agent.jvmapi.vmGlobal
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@Suppress("unused_parameter")
@CName("Agent_OnLoad")
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): Int = memScoped {
    vmGlobal.value = vmPointer.freeze()
    val vm = vmPointer.pointed
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    jvmti.value = jvmtiEnvPtr.value.freeze()
    Agent.agentOnLoad(options)
}

@Suppress("unused_parameter")
@CName("Agent_OnUnload")
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>) = Agent.agentOnUnload()

@CName("checkEx")
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
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
