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
package com.epam.drill.core.plugin.loader

import mu.KotlinLogging
import com.epam.drill.addPluginToStorage
import com.epam.drill.agent.DataService
import com.epam.drill.common.Family
import com.epam.drill.common.PluginMetadata
import com.epam.drill.jvmapi.AttachNativeThreadToJvm
import com.epam.drill.jvmapi.gen.GetObjectClass
import com.epam.drill.jvmapi.gen.NewGlobalRef
import com.epam.drill.jvmapi.gen.jobject

@Suppress("UNCHECKED_CAST")
fun loadJvmPlugin(pluginConfig: PluginMetadata) {
    val logger = KotlinLogging.logger("com.epam.drill.core.plugin.loader.PluginLoader.loadJvmPlugin")
    AttachNativeThreadToJvm()
    try {
        val pluginId = pluginConfig.id
        val agentPart = DataService.createAgentPart(pluginId) as? jobject
        val pluginApiClass = GetObjectClass(agentPart)!!
        val agentPartRef = NewGlobalRef(agentPart)!!
        val plugin = when (pluginConfig.family) {
            Family.INSTRUMENTATION -> InstrumentationNativePlugin(pluginId, pluginApiClass, agentPartRef, pluginConfig)
            Family.GENERIC -> GenericNativePlugin(pluginId, pluginApiClass, agentPartRef, pluginConfig)
        }
        addPluginToStorage(plugin)
        plugin.load(false)
    } catch (ex: Exception) {
        logger.error(ex) { "Fatal error processing plugin: id=${pluginConfig.id}, name=${pluginConfig.name}" }
    }
}
