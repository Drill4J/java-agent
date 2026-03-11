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
package com.epam.drill.agent.jvmapi

import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import com.epam.drill.agent.jvmapi.gen.JNIEnvVar
import com.epam.drill.agent.jvmapi.gen.JavaVMVar
import com.epam.drill.agent.jvmapi.gen.jvmtiEnvVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.concurrent.ThreadLocal

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val vmGlobal = AtomicReference<CPointer<JavaVMVar>?>(null).freeze()

@OptIn(ExperimentalForeignApi::class)
@SharedImmutable
val jvmti = AtomicReference<CPointer<jvmtiEnvVar>?>(null).freeze()

@OptIn(ExperimentalForeignApi::class)
@ThreadLocal
val env: CPointer<JNIEnvVar> by lazy {
    memScoped {
        val vm = vmGlobal.value!!
        val vmFns = vm.pointed.value!!.pointed
        val jvmtiEnvPtr = alloc<CPointerVar<JNIEnvVar>>()
        vmFns.AttachCurrentThread!!(vm, jvmtiEnvPtr.ptr.reinterpret(), null)
        jvmtiEnvPtr.value!!
    }
}
