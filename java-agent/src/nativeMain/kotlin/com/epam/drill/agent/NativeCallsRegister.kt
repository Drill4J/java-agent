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
package com.epam.drill.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import com.epam.drill.agent.configuration.agentConfig
import com.epam.drill.agent.configuration.agentParameters
import com.epam.drill.agent.jvmti.sendToSocket
import com.epam.drill.jvmapi.withJString
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.NativeCallsRegister")

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_agent_JvmModuleMessageSender_send")
fun sendFromJava(envs: JNIEnv, thiz: jobject, jpluginId: jstring, jmessage: jstring) = withJString {
    sendToSocket(jpluginId.toKString(), jmessage.toKString())
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_NativeCalls_getPackagePrefixes")
fun getPackagePrefixes(): jstring? {
    val packagesPrefixes = agentConfig.packagesPrefixes.packagesPrefixes
    return NewStringUTF(packagesPrefixes.joinToString(", "))
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_NativeCalls_getScanClassPath")
fun getScanClassPath(): jstring? {
    return NewStringUTF(agentParameters.scanClassPath)
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test2code_NativeCalls_waitClassScanning")
fun waitClassScanning() = runBlocking {
    val classScanDelay = agentParameters.classScanDelay - Agent.startTimeMark.elapsedNow()
    if (classScanDelay.isPositive()) {
        logger.debug { "Waiting class scan delay ($classScanDelay left)..." }
        delay(classScanDelay)
    }
}
