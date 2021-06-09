package com.epam.drill.request

import com.epam.drill.*
import com.epam.drill.core.plugin.loader.*

actual object PluginExtension {
    actual fun processServerRequest(){
        pstorage.values.filterIsInstance<GenericNativePlugin>().forEach {
            it.processServerRequest()
        }
    }
}
