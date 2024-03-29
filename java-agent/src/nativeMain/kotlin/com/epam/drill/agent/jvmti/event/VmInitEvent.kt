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
package com.epam.drill.agent.jvmti.event

import kotlinx.cinterop.CPointer
import mu.KotlinLogging
import com.epam.drill.agent.Agent
import com.epam.drill.agent.JvmModuleLoader
import com.epam.drill.agent.globalCallbacks
import com.epam.drill.agent.configuration.adminAddress
import com.epam.drill.agent.configuration.agentParameters
import com.epam.drill.agent.configuration.configureHttp
import com.epam.drill.agent.configuration.defaultJvmLoggingConfiguration
import com.epam.drill.agent.configuration.updateJvmLoggingConfiguration
import com.epam.drill.agent.configuration.updatePackagePrefixesConfiguration
import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.agent.websocket.WsClient
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.JVMTI_ENABLE
import com.epam.drill.jvmapi.gen.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK
import com.epam.drill.jvmapi.gen.SetEventNotificationMode
import com.epam.drill.jvmapi.gen.jthread
import com.epam.drill.jvmapi.gen.jvmtiEnvVar

private val logger = KotlinLogging.logger("com.epam.drill.agent.jvmti.event.VmInitEvent")

@Suppress("UNUSED_PARAMETER")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

    defaultJvmLoggingConfiguration()
    updateJvmLoggingConfiguration()

    if (Agent.isHttpHookEnabled) {
        logger.info { "run with http hook" }
        configureHttp()
    } else {
        logger.warn { "run without http hook" }
    }

    globalCallbacks()
    updatePackagePrefixesConfiguration()
    loadJvmModule("test2code")
    RequestHolder.init(isAsync = agentParameters.isAsyncApp)
    WsClient.connect(adminAddress.toString(), true)
}

@Suppress("UNCHECKED_CAST")
private fun loadJvmModule(id: String) {
    try {
        JvmModuleLoader.loadJvmModule(id).load()
    } catch (ex: Exception) {
        logger.error(ex) { "Fatal error processing plugin: id=${id}" }
    }
}
