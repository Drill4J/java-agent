/**
 * Copyright 2020 EPAM Systems
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
import com.epam.drill.core.plugin.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*

@Suppress("LeakingThis")
open class GenericNativePlugin(
    pluginId: String,
    val pluginApiClass: jclass,
    val userPlugin: jobject,
    pluginConfig: PluginMetadata,
) : AgentPart<Any>(
    pluginId,
    NopAgentContext,
    NopPluginSender,
    Logging
) {
    private val pluginLogger = Logging.logger("GenericNativePlugin $pluginId")

    init {
        updateRawConfig(pluginConfig.config)
        javaEnabled(pluginConfig.enabled)
    }

    override suspend fun doRawAction(rawAction: String) {
        DataService.doRawActionBlocking(userPlugin, rawAction)
    }

    override fun isEnabled() = pluginConfigById(id).enabled

    override fun setEnabled(enabled: Boolean) {
        javaEnabled(enabled)
        val pluginConfigById = pluginConfigById(id)
        addPluginConfig(pluginConfigById.copy(enabled = enabled))
    }

    private fun javaEnabled(value: Boolean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, "setEnabled", "(Z)V"),
            nativeHeap.allocArray(1.toLong()) {
                z = if (value) 1.toUByte() else 0.toUByte()
            })
    }

    override fun on() {
        pluginLogger.debug { "on" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::on.name, "()V"), null
        )
    }

    override fun off() {
        pluginLogger.debug { "off" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::off.name, "()V"), null
        )
    }

    override fun load(on: Boolean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::load.name, "(Z)V"),
            nativeHeap.allocArray(1.toLong()) {
                z = if (on) 1.toUByte() else 0.toUByte()
            })

    }

    override fun unload(unloadReason: UnloadReason) = memScoped {
        val findClass = FindClass(UnloadReason::class.jniName())
        val getStaticFieldID =
            GetStaticFieldID(findClass, unloadReason.name, UnloadReason::class.jniParamName())
        val getStaticObjectField = GetStaticObjectField(findClass, getStaticFieldID)
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::unload.name, "(${UnloadReason::class.jniParamName()})V"),
            allocArray(1.toLong()) {
                l = getStaticObjectField
            }
        )
    }

    override fun updateRawConfig(data: String) {
        notifyJavaPart(data)
    }

    private fun notifyJavaPart(data: String) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::updateRawConfig.name, "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                l = NewStringUTF(data)
            })
    }

    fun processServerRequest() {
        val methodID = GetMethodID(pluginApiClass, GenericNativePlugin::processServerRequest.name, "()V")
        methodID?.let {
            CallVoidMethodA(userPlugin, it, null)
        }
    }

    fun processServerResponse() {
        val methodID = GetMethodID(pluginApiClass, GenericNativePlugin::processServerResponse.name, "()V")
        methodID?.let {
            CallVoidMethodA(userPlugin, it, null)
        }
    }

    override fun initPlugin() = TODO()
    override fun destroyPlugin(unloadReason: UnloadReason) = TODO()
    override suspend fun doAction(action: Any) = TODO()
    override fun parseAction(rawAction: String) = TODO()
}

private object NopAgentContext : AgentContext {
    override fun get(key: String): String? = null
    override fun invoke(): String? = null
}

private object NopPluginSender : Sender {
    override fun send(pluginId: String, message: String) = Unit
}
