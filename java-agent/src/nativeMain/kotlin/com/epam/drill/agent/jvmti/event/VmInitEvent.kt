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
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import com.epam.drill.agent.JvmModuleLoader
import com.epam.drill.agent.closeSession
import com.epam.drill.agent.configuration.AgentLoggingConfiguration
import com.epam.drill.agent.configuration.JavaAgentConfiguration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.configuration.configureHttp
import com.epam.drill.agent.drillRequest
import com.epam.drill.agent.request.DrillRequest
import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.agent.request.RequestProcessor
import com.epam.drill.agent.sessionStorage
import com.epam.drill.agent.transport.JvmModuleMessageSender
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

    AgentLoggingConfiguration.defaultJvmLoggingConfiguration()
    AgentLoggingConfiguration.updateJvmLoggingConfiguration()
    JavaAgentConfiguration.initializeJvm()

    if (JavaAgentConfiguration.parameters[ParameterDefinitions.HTTP_HOOK_ENABLED]) {
        logger.info { "vmInitEvent: Run with http hook" }
        configureHttp()
    } else {
        logger.warn { "vmInitEvent: Run without http hook" }
    }

    registerGlobalCallbacks()
    loadJvmModule("com.epam.drill.test2code.Test2Code")
    JvmModuleMessageSender.sendAgentMetadata()
}

private fun registerGlobalCallbacks() {
    sessionStorage = {
        RequestHolder.store(ProtoBuf.encodeToByteArray(DrillRequest.serializer(), it))
    }
    closeSession = {
        RequestProcessor.processServerResponse()
        RequestHolder.closeSession()
    }
    drillRequest = {
        RequestHolder.dump()?.let { ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), it) }
    }
    RequestHolder.init(JavaAgentConfiguration.parameters[ParameterDefinitions.IS_ASYNC_APP])
}

private fun loadJvmModule(id: String) = runCatching { JvmModuleLoader.loadJvmModule(id).load() }
    .onFailure { logger.error(it) { "Fatal error processing plugin: id=${id}" } }
