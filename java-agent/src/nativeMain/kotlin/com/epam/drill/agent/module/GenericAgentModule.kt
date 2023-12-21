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
package com.epam.drill.agent.module

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.common.*
import com.epam.drill.common.agent.*
import com.epam.drill.common.agent.transport.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

open class GenericAgentModule(
    pluginId: String,
    val pluginApiClass: jclass,
    val userPlugin: jobject
) : AgentModule<Any>(
    pluginId,
    NopAgentContext,
    NopPluginSender
) {

    override fun load() {
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentModule<*>::load.name, "()V"), null
        )

    }

    override fun onConnect() {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, GenericAgentModule::onConnect.name, "()V"),
            null
        )
    }

    fun processServerRequest() {
        val methodID = GetMethodID(pluginApiClass, GenericAgentModule::processServerRequest.name, "()V")
        methodID?.let {
            CallVoidMethodA(userPlugin, it, null)
        }
    }

    fun processServerResponse() {
        val methodID = GetMethodID(pluginApiClass, GenericAgentModule::processServerResponse.name, "()V")
        methodID?.let {
            CallVoidMethodA(userPlugin, it, null)
        }
    }

}

private object NopAgentContext : AgentContext {
    override fun get(key: String): String? = null
    override fun invoke(): String? = null
}

private object NopPluginSender : AgentMessageSender {
    override val available: Boolean = false
    override fun send(destination: AgentMessageDestination, message: AgentMessage) = NopResponseStatus()
}

private class NopResponseStatus: ResponseStatus {
    override val success = false
    override val statusObject = null
}
