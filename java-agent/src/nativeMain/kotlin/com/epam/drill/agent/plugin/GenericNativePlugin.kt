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
package com.epam.drill.agent.plugin

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.common.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import mu.KotlinLogging

open class GenericNativePlugin(
    pluginId: String,
    val pluginApiClass: jclass,
    val userPlugin: jobject
) : AgentPart<Any>(
    pluginId,
    NopAgentContext,
    NopPluginSender
) {

    private val logger = KotlinLogging.logger(GenericNativePlugin::class.qualifiedName!!)
    private val pluginApiClassName = pluginApiClass.signature()
        .removePrefix("L").removeSuffix(";").replace("<L", "<").replace(";>", ">").replace("/", ".")

    override fun doRawAction(rawAction: String): Any {
        logger.debug { "doRawAction: $rawAction" }
        return CallObjectMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*>::doRawAction.name, "(Ljava/lang/String;)Ljava/lang/Object;"),
            nativeHeap.allocArray(1L) {
                l = NewStringUTF(rawAction)
            }
        )!!
    }

    override fun on() {
        logger.debug { "on(), pluginApiClass=$pluginApiClassName" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::on.name, "()V"), null
        )
    }

    override fun off() {
        logger.debug { "off(), pluginApiClass=$pluginApiClassName" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::off.name, "()V"), null
        )
    }

    override fun load() {
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*>::load.name, "()V"), null
        )

    }

    override fun onConnect() {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, GenericNativePlugin::onConnect.name, "()V"),
            null
        )
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

    override fun doAction(action: Any) = TODO()
    override fun parseAction(rawAction: String) = TODO()
}

private object NopAgentContext : AgentContext {
    override fun get(key: String): String? = null
    override fun invoke(): String? = null
}

private object NopPluginSender : Sender {
    override fun send(pluginId: String, message: String) = Unit
}
