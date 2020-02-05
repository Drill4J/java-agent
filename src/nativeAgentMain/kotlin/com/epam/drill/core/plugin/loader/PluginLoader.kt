package com.epam.drill.core.plugin.loader

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import mu.*
import kotlin.collections.set


@SharedImmutable
val plLogger = KotlinLogging.logger("plLogger")

fun loadPlugin(pluginFilePath: String, pluginConfig: PluginMetadata) {
    AttachNativeThreadToJvm()
    AddToSystemClassLoaderSearch(pluginFilePath)
    plLogger.warn { "System classLoader extends by '$pluginFilePath' path" }
    try {
        val pluginId = pluginConfig.id
        val initializerClass = FindClass("com/epam/drill/ws/ClassLoadingUtil")
        val selfMethodId: jfieldID? =
            GetStaticFieldID(initializerClass, "INSTANCE", "Lcom/epam/drill/ws/ClassLoadingUtil;")
        val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
        val retrieveApiClass: jmethodID? =
            GetMethodID(initializerClass, "retrieveApiClass", "(Ljava/lang/String;)Ljava/lang/Class;")
        val pluginApiClass: jclass = CallObjectMethod(initializer, retrieveApiClass, NewStringUTF(pluginFilePath))!!

        val getPayloadClass: jmethodID? =
            GetMethodID(initializerClass, "getPluginPayload", "(Ljava/lang/String;)Lcom/epam/drill/plugin/api/PluginPayload;")
        val payload: jobject = CallObjectMethod(initializer, getPayloadClass, NewStringUTF(pluginId))!!

        val userPlugin: jobject =
            NewGlobalRef(
                NewObjectA(
                    pluginApiClass,
                    GetMethodID(pluginApiClass, "<init>", "(Lcom/epam/drill/plugin/api/PluginPayload;)V"),
                    nativeHeap.allocArray(1.toLong()) {
                        l = payload
                    }
                )
            )!!

        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> {
                val inst = InstrumentationNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig)
                exec {
                    pstorage[pluginConfig.id] = inst
                }
                inst.retransform()
            }
            Family.GENERIC -> {
                GenericNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig).apply {
                    exec {
                        pstorage[this@apply.id] = this@apply
                    }
                }
            }
        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file $pluginFilePath. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of $pluginFilePath jar... Error: ${ex.message} ${ex.printStackTrace()}" }
        }
    }
}

