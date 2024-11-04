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
package com.epam.drill.agent.request.jvm

import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.jvmapi.callNativeStringMethod
import com.epam.drill.agent.jvmapi.gen.JNIEnv
import com.epam.drill.agent.jvmapi.gen.jobject
import com.epam.drill.agent.jvmapi.gen.jstring
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_adminAddressHeader")
fun adminAddressHeader(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::adminAddressHeader)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_adminAddressValue")
fun retrieveAdminAddress(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::adminAddressValue)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_sessionHeader")
fun sessionHeaderPattern(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::sessionHeader)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_agentIdHeader")
fun idHeaderConfigKey(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::agentIdHeader)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_agentIdHeaderValue")
fun idHeaderConfigValue(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::agentIdHeaderValue)
