package com.epam.drill.plugin

import com.epam.drill.common.*
import com.epam.drill.plugin.api.processing.*
import java.util.concurrent.*


val storage: MutableMap<String, AgentPart<*, *>>
    get() = ConcurrentHashMap()


fun AgentPart<*, *>.actualPluginConfig(): PluginMetadata {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}