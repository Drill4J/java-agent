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
