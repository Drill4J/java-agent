package com.epam.drill.plugin

import com.epam.drill.plugin.api.processing.*

object PluginSender : Sender {
    external override fun send(pluginId: String, message: String)
}
