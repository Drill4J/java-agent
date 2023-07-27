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
package com.epam.drill.core.callbacks.vminit

import kotlinx.cinterop.CPointer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import com.epam.drill.addPluginToStorage
import com.epam.drill.adminAddress
import com.epam.drill.pstorage
import com.epam.drill.agent.DataService
import com.epam.drill.agent.defaultJvmLoggingConfiguration
import com.epam.drill.agent.updateJvmLoggingConfiguration
import com.epam.drill.agent.config
import com.epam.drill.agent.state
import com.epam.drill.agent.updatePackagePrefixesConfiguration
import com.epam.drill.common.Family
import com.epam.drill.core.Agent
import com.epam.drill.core.globalCallbacks
import com.epam.drill.core.plugin.loader.GenericNativePlugin
import com.epam.drill.core.plugin.loader.InstrumentationNativePlugin
import com.epam.drill.core.transport.configureHttp
import com.epam.drill.core.ws.WsSocket
import com.epam.drill.jvmapi.gen.GetObjectClass
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.JVMTI_ENABLE
import com.epam.drill.jvmapi.gen.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK
import com.epam.drill.jvmapi.gen.NewGlobalRef
import com.epam.drill.jvmapi.gen.SetEventNotificationMode
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jthread
import com.epam.drill.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.request.RequestHolder

private val logger = KotlinLogging.logger("com.epam.drill.core.callbacks.vminit.VmInitEvent")

@Suppress("UNUSED_PARAMETER")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
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
    loadJvmModule("test2code", Family.INSTRUMENTATION)
    WsSocket().connect(adminAddress.toString())
    RequestHolder.init(isAsync = config.isAsyncApp)
    runBlocking {
        for (i in 1..5) {
            logger.info { "Agent is not alive. Waiting for package settings from $adminAddress..." }
            delay(500L)
            if (state.alive) {
                logger.info { "Agent is alive! Waiting for loading of at least one plugin..." }
                while (pstorage.none()) {
                    delay(500L)
                }
                logger.info {
                    "At least on plugin is loaded (plugins ${pstorage.keys.toList()}), continue vm initializing."
                }
                break
            }
        }
        if (pstorage.none()) {
            logger.info { "No plugins loaded from $adminAddress." }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun loadJvmModule(id: String, family: Family) {
    try {
        val agentPart = DataService.createAgentPart(id) as? jobject
        val pluginApiClass = NewGlobalRef(GetObjectClass(agentPart))!!
        val agentPartRef = NewGlobalRef(agentPart)!!
        val plugin = when (family) {
            Family.INSTRUMENTATION -> InstrumentationNativePlugin(id, pluginApiClass, agentPartRef)
            Family.GENERIC -> GenericNativePlugin(id, pluginApiClass, agentPartRef)
        }
        addPluginToStorage(plugin)
        plugin.load()
    } catch (ex: Exception) {
        logger.error(ex) { "Fatal error processing plugin: id=${id}" }
    }
}
