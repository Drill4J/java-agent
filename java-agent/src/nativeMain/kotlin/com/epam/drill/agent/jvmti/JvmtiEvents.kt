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

import kotlinx.cinterop.CPointer
import com.epam.drill.agent.Agent
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.jthread
import com.epam.drill.jvmapi.gen.jvmtiEnvVar

@Suppress("unused_parameter")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = Agent.agentOnVmInit()

@Suppress("unused_parameter")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) = Agent.agentOnVmDeath()

//@Suppress("unused_parameter")
//fun classFileLoadHook(
//    jvmtiEnv: CPointer<jvmtiEnvVar>?,
//    jniEnv: CPointer<JNIEnvVar>?,
//    classBeingRedefined: jclass?,
//    loader: jobject?,
//    clsName: CPointer<ByteVar>?,
//    protectionDomain: jobject?,
//    classDataLen: jint,
//    classData: CPointer<UByteVar>?,
//    newClassDataLen: CPointer<jintVar>?,
//    newData: CPointer<CPointerVar<UByteVar>>?,
//) = ClassFileLoadHook.invoke(
//    classBeingRedefined,
//    loader,
//    clsName,
//    protectionDomain,
//    classDataLen,
//    classData,
//    newClassDataLen,
//    newData
//)
