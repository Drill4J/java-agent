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
package com.epam.drill.agent.configuration.jvm

import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.callNativeVoidMethod
import com.epam.drill.jvmapi.callNativeVoidMethodWithString
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_WsConfiguration_getAgentConfigHexString")
fun getAgentConfigHexString(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, WsConfiguration::getAgentConfigHexString)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_WsConfiguration_setRequestPattern")
fun setRequestPattern(env: JNIEnv, thiz: jobject, pattern: String) =
    callNativeVoidMethodWithString(env, thiz, WsConfiguration::setRequestPattern, pattern)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_WsConfiguration_generateAgentConfigInstanceId")
fun generateAgentConfigInstanceId(env: JNIEnv, thiz: jobject) =
    callNativeVoidMethod(env, thiz, WsConfiguration::generateAgentConfigInstanceId)
