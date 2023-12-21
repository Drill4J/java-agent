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

import com.epam.drill.agent.configuration.TransportConfiguration
import com.epam.drill.jvmapi.callNativeByteArrayMethod
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jbyteArray
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getAgentConfigHexString")
fun getAgentConfigBytes(env: JNIEnv, thiz: jobject): jbyteArray? =
    callNativeByteArrayMethod(env, thiz, TransportConfiguration::getAgentConfigBytes)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getAgentId")
fun getAgentId(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, TransportConfiguration::getAgentId)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getBuildVersion")
fun getBuildVersion(env: JNIEnv, thiz: jobject): jstring? =
    callNativeStringMethod(env, thiz, TransportConfiguration::getBuildVersion)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getSslTruststore")
fun getSslTruststore(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, TransportConfiguration::getSslTruststore)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getSslTruststorePassword")
fun getSslTruststorePassword(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, TransportConfiguration::getSslTruststorePassword)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getDrillInstallationDir")
fun getDrillInstallationDir(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, TransportConfiguration::getDrillInstallationDir)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getCoverageRetentionLimit")
fun getTransportCoverageRetentionLimit(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, TransportConfiguration::getCoverageRetentionLimit)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_configuration_TransportConfiguration_getAdminAddress")
fun getAdminAddress(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, TransportConfiguration::getAdminAddress)
