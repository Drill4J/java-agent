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

import kotlin.reflect.KClass
import com.epam.drill.agent.jvmapi.gen.FindClass
import com.epam.drill.agent.jvmapi.gen.GetMethodID
import com.epam.drill.agent.jvmapi.gen.GetStaticFieldID
import com.epam.drill.agent.jvmapi.gen.GetStaticObjectField
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun getObjectMethod(clazz: KClass<out Any>, method: String, signature: String) = run {
    val className = clazz.qualifiedName!!.replace(".", "/")
    val classRef = FindClass(className)
    val methodId = GetMethodID(classRef, method, signature)
    val instanceId = GetStaticFieldID(classRef, "INSTANCE", "L$className;")
    val instaceRef = GetStaticObjectField(classRef, instanceId)
    instaceRef to methodId
}

@OptIn(ExperimentalForeignApi::class)
fun getObjectVoidMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()V")

@OptIn(ExperimentalForeignApi::class)
fun getObjectVoidMethodWithBoolean(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Z)V")

@OptIn(ExperimentalForeignApi::class)
fun getObjectVoidMethodWithInt(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(I)V")

@OptIn(ExperimentalForeignApi::class)
fun getObjectVoidMethodWithString(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)V")

@OptIn(ExperimentalForeignApi::class)
fun getObjectVoidMethodWithByteArray(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "([B)V")

@OptIn(ExperimentalForeignApi::class)
fun getObjectIntMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()I")

@OptIn(ExperimentalForeignApi::class)
fun getObjectObjectMethodWithString(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;)Ljava/lang/Object;")

@OptIn(ExperimentalForeignApi::class)
fun getObjectStringMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()Ljava/lang/String;")

@OptIn(ExperimentalForeignApi::class)
fun getObjectByteArrayMethod(clazz: KClass<out Any>, method: String) =
    getObjectMethod(clazz, method, "()[B")
