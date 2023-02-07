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

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.common.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*

fun loadJvmPlugin(pluginFilePath: String, pluginConfig: PluginMetadata) {
    val logger = Logging.logger("Plugin ${pluginConfig.id}")
    AttachNativeThreadToJvm()
    AddToSystemClassLoaderSearch(pluginFilePath)
    logger.info { "System classLoader extends by '$pluginFilePath' path" }
    try {
        val pluginId = pluginConfig.id

        @Suppress("UNCHECKED_CAST")
        val agentPart = DataService.createAgentPart(pluginId, pluginFilePath) as? jobject
        val pluginApiClass = GetObjectClass(agentPart)!!
        val agentPartRef: jobject = NewGlobalRef(agentPart)!!

        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> InstrumentationNativePlugin(pluginId, pluginApiClass, agentPartRef, pluginConfig)
            Family.GENERIC -> GenericNativePlugin(pluginId, pluginApiClass, agentPartRef, pluginConfig)
        }.run {
            addPluginToStorage(this)
            load(false)

        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException -> logger.error(ex) { "Can't load plugin file $pluginFilePath." }
            else -> logger.error(ex) { "Fatal error processing $pluginFilePath." }
        }
    }
}
