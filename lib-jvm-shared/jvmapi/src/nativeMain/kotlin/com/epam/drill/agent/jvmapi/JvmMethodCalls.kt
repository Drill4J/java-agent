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

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlinx.cinterop.toKString
import com.epam.drill.agent.jvmapi.gen.CallIntMethod
import com.epam.drill.agent.jvmapi.gen.CallObjectMethod
import com.epam.drill.agent.jvmapi.gen.CallVoidMethod
import com.epam.drill.agent.jvmapi.gen.GetStringUTFChars
import com.epam.drill.agent.jvmapi.gen.NewStringUTF
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun callObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectVoidMethod(clazz, method).run {
        CallVoidMethod(this.first, this.second)
    }

fun callObjectVoidMethod(clazz: KClass<out Any>, method: KCallable<Unit>) =
    callObjectVoidMethod(clazz, method.name)

@OptIn(ExperimentalForeignApi::class)
fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: String, bool: Boolean) =
    getObjectVoidMethodWithBoolean(clazz, method).run {
        CallVoidMethod(this.first, this.second, bool)
    }

fun callObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: KCallable<Unit>, bool: Boolean) =
    callObjectVoidMethodWithBoolean(clazz, method.name, bool)

@OptIn(ExperimentalForeignApi::class)
fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: String, int: Int) =
    getObjectVoidMethodWithInt(clazz, method).run {
        CallVoidMethod(this.first, this.second, int)
    }

fun callObjectVoidMethodWithInt(clazz: KClass<out Any>, method: KCallable<Unit>, int: Int) =
    callObjectVoidMethodWithInt(clazz, method.name, int)

@OptIn(ExperimentalForeignApi::class)
fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectVoidMethodWithString(clazz, method).run {
        CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

fun callObjectVoidMethodWithString(clazz: KClass<out Any>, method: KCallable<Unit>, string: String?) =
    callObjectVoidMethodWithString(clazz, method.name, string)

@OptIn(ExperimentalForeignApi::class)
fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: String, bytes: ByteArray) =
    getObjectVoidMethodWithByteArray(clazz, method).run {
        CallVoidMethod(this.first, this.second, toJByteArray(bytes))
    }

fun callObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: KCallable<Unit>, bytes: ByteArray) =
    callObjectVoidMethodWithByteArray(clazz, method.name, bytes)

@OptIn(ExperimentalForeignApi::class)
fun callObjectIntMethod(clazz: KClass<out Any>, method: String) =
    getObjectIntMethod(clazz, method).run {
        CallIntMethod(this.first, this.second)
    }

fun callObjectIntMethod(clazz: KClass<out Any>, method: KCallable<Int>) =
    callObjectIntMethod(clazz, method.name)

@OptIn(ExperimentalForeignApi::class)
fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
    getObjectObjectMethodWithString(clazz, method).run {
        CallObjectMethod(this.first, this.second, string?.let(::NewStringUTF))
    }

@OptIn(ExperimentalForeignApi::class)
@Suppress("unused")
fun callObjectObjectMethodWithString(clazz: KClass<out Any>, method: KCallable<Any?>, string: String?) =
    callObjectObjectMethodWithString(clazz, method.name, string)

@OptIn(ExperimentalForeignApi::class)
fun callObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectStringMethod(clazz, method).run {
        CallObjectMethod(this.first, this.second)?.let { GetStringUTFChars(it, null)?.toKString() }
    }

fun callObjectStringMethod(clazz: KClass<out Any>, method: KCallable<String?>) =
    callObjectStringMethod(clazz, method.name)

@OptIn(ExperimentalForeignApi::class)
fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: String) =
    getObjectByteArrayMethod(clazz, method).run {
        CallObjectMethod(this.first, this.second)?.let(::toByteArray)
    }

fun callObjectByteArrayMethod(clazz: KClass<out Any>, method: KCallable<ByteArray?>) =
    callObjectByteArrayMethod(clazz, method.name)
