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

import com.epam.drill.agent.request.RequestProcessor
import com.epam.drill.jvmapi.callNativeVoidMethod
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_RequestProcessor_processServerRequest")
fun processServerRequest(env: JNIEnv, thiz: jobject): Unit =
    callNativeVoidMethod(env, thiz, RequestProcessor::processServerRequest)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_request_RequestProcessor_processServerResponse")
fun processServerResponse(env: JNIEnv, thiz: jobject): Unit =
    callNativeVoidMethod(env, thiz, RequestProcessor::processServerResponse)
