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
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_adminAddressHeader")
fun adminAddressHeader(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::adminAddressHeader)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_retrieveAdminAddress")
fun retrieveAdminAddress(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::retrieveAdminAddress)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_sessionHeaderPattern")
fun sessionHeaderPattern(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::sessionHeaderPattern)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_idHeaderConfigKey")
fun idHeaderConfigKey(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::idHeaderConfigKey)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_HeadersRetriever_idHeaderConfigValue")
fun idHeaderConfigValue(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, HeadersRetriever::idHeaderConfigValue)
