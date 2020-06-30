package com.epam.drill.core.plugin.loader

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*


@SharedImmutable
val plLogger = Logging.logger("plLogger")

fun dataService(): Pair<jclass?, jobject?> {
    val className = "com/epam/drill/agent/DataService"
    val initializerClass = FindClass(className)
    val selfMethodId: jfieldID? =
        GetStaticFieldID(initializerClass, "INSTANCE", "L$className;")
    val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
    return initializerClass to initializer
}

fun loadPluginForJvm(pluginFilePath: String, pluginConfig: PluginMetadata) {
    AttachNativeThreadToJvm()
    AddToSystemClassLoaderSearch(pluginFilePath)
    plLogger.info { "System classLoader extends by '$pluginFilePath' path" }
    try {
        val pluginId = pluginConfig.id
        val (serviceClass, service) = dataService()
        val retrieveApiClass: jmethodID? =
            GetMethodID(serviceClass, "retrieveApiClass", "(Ljava/lang/String;)Ljava/lang/Class;")
        val pluginApiClass: jclass = CallObjectMethod(service, retrieveApiClass, NewStringUTF(pluginFilePath))!!

        val pluginPayloadClassName = "com/epam/drill/plugin/api/PluginPayload"
        val getPayloadClass: jmethodID? =
            GetMethodID(serviceClass, "getPluginPayload", "(Ljava/lang/String;)L$pluginPayloadClassName;")
        val payload: jobject = CallObjectMethod(service, getPayloadClass, NewStringUTF(pluginId))!!

        val userPlugin: jobject =
            NewGlobalRef(
                NewObjectA(
                    pluginApiClass,
                    GetMethodID(pluginApiClass, "<init>", "(L$pluginPayloadClassName;)V"),
                    nativeHeap.allocArray(1.toLong()) {
                        l = payload
                    }
                )
            )!!

        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> InstrumentationNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig)
            Family.GENERIC -> GenericNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig)
        }.run {
            pstorage[pluginConfig.id] = this
            load(false)

        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file $pluginFilePath. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of $pluginFilePath jar... Error: ${ex.message} ${ex.printStackTrace()}" }
        }
    }
}
