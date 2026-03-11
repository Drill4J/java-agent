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

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toByte
import com.epam.drill.agent.jvmapi.gen.JNIEnv
import com.epam.drill.agent.jvmapi.gen.NewStringUTF
import com.epam.drill.agent.jvmapi.gen.jobject
import com.epam.drill.agent.jvmapi.gen.jstring
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNUSED_PARAMETER")
fun callNativeVoidMethod(env: JNIEnv, thiz: jobject, method: () -> Unit) = memScoped {
    method()
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNUSED_PARAMETER")
fun callNativeVoidMethodWithString(env: JNIEnv, thiz: jobject, method: (String?) -> Unit, string: jstring?) = memScoped {
    withStringsRelease {
        method(string?.let(this::toKString))
    }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNUSED_PARAMETER")
fun callNativeBooleanMethod(env: JNIEnv, thiz: jobject, method: () -> Boolean?) = memScoped {
    method()?.toByte()?.toUByte()
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNUSED_PARAMETER")
fun callNativeStringMethod(env: JNIEnv, thiz: jobject, method: () -> String?) = memScoped {
    NewStringUTF(method())
}
