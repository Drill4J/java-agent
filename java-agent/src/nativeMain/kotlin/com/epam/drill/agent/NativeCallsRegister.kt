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

import kotlin.native.concurrent.*
import kotlin.time.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import mu.*
import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.configuration.*
import com.epam.drill.agent.jvmti.*
import com.epam.drill.agent.jvmti.event.*
import com.epam.drill.api.*
import com.epam.drill.common.classloading.*
import com.epam.drill.core.messanger.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.NativeCallsRegister")

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_agent_JvmModuleMessageSender_send")
fun sendFromJava(envs: JNIEnv, thiz: jobject, jpluginId: jstring, jmessage: jstring) = withJString {
    sendToSocket(jpluginId.toKString(), jmessage.toKString())
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_NativeCalls_getPackagePrefixes")
fun GetPackagePrefixes(): jstring? {
    val packagesPrefixes = agentConfig.packagesPrefixes.packagesPrefixes
    return NewStringUTF(packagesPrefixes.joinToString(", "))
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_NativeCalls_getScanClassPath")
fun GetScanClassPath(): jstring? {
    return NewStringUTF(agentParameters.scanClassPath)
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_agent_NativeCalls_waitClassScanning")
fun WaitClassScanning() = runBlocking {
    val classScanDelay = agentParameters.classScanDelay - agentStartTimeMark.elapsedNow()
    if (classScanDelay.isPositive()) {
        logger.debug { "Waiting class scan delay ($classScanDelay left)..." }
        delay(classScanDelay)
    }
}
