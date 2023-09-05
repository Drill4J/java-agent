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

import com.epam.drill.agent.configuration.JvmModuleConfiguration
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.callNativeLongMethod
import com.epam.drill.jvmapi.callNativeVoidMethod
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring
import com.epam.drill.jvmapi.gen.jlong

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_JvmModuleConfiguration_getPackagePrefixes")
fun getPackagePrefixes(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, JvmModuleConfiguration::getPackagePrefixes)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_JvmModuleConfiguration_getScanClassPath")
fun getScanClassPath(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, JvmModuleConfiguration::getScanClassPath)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_JvmModuleConfiguration_waitClassScanning")
fun waitClassScanning(env: JNIEnv, thiz: jobject) =
    callNativeVoidMethod(env, thiz, JvmModuleConfiguration::waitClassScanning)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_JvmModuleConfiguration_getCoverageRetentionLimit")
fun getCoverageRetentionLimit(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, JvmModuleConfiguration::getCoverageRetentionLimit)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_JvmModuleConfiguration_getSendCoverageInterval")
fun getSendCoverageInterval(env: JNIEnv, thiz: jobject): jlong =
    callNativeLongMethod(env, thiz, JvmModuleConfiguration::getSendCoverageInterval)
