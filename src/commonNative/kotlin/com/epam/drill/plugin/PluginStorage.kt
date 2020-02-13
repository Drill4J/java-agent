package com.epam.drill.plugin

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.plugin.*
import com.epam.drill.plugin.api.processing.*

val storage: MutableMap<String, AgentPart<*, *>>
    get() = exec { pstorage }


fun AgentPart<*, *>.actualPluginConfig() = pluginConfigById(this.id)

object PluginManager {

    fun addPlugin(plugin: AgentPart<*, *>) {
        storage[plugin.id] = plugin
    }

    operator fun get(id: String) = storage[id]
    operator fun get(id: Family) = storage.values.groupBy { it.actualPluginConfig().family }[id]
}
