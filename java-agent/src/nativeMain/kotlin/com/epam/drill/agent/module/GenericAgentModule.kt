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

import com.epam.drill.agent.request.RequestProcessor
import com.epam.drill.common.agent.AgentContext
import com.epam.drill.common.agent.AgentModule
import com.epam.drill.common.agent.configuration.AgentConfiguration
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.jvmapi.gen.CallVoidMethod
import com.epam.drill.jvmapi.gen.GetMethodID
import com.epam.drill.jvmapi.gen.jclass
import com.epam.drill.jvmapi.gen.jobject

open class GenericAgentModule(
    moduleId: String,
    moduleClass: jclass,
    protected val moduleObject: jobject
) : AgentModule(
    moduleId,
    NopAgentContext(),
    NopMessageSender(),
    NopConfiguration()
) {

    private val loadMethod = GetMethodID(moduleClass, AgentModule::load.name, "()V")
    private val onConnectMethod = GetMethodID(moduleClass, AgentModule::onConnect.name, "()V")
    private val processServerRequestMethod = GetMethodID(moduleClass, RequestProcessor::processServerRequest.name, "()V")
    private val processServerResponseMethod = GetMethodID(moduleClass, RequestProcessor::processServerResponse.name, "()V")

    override fun load() = CallVoidMethod(moduleObject, loadMethod)

    override fun onConnect() = CallVoidMethod(moduleObject, onConnectMethod)

    fun processServerRequest() = processServerRequestMethod?.let { CallVoidMethod(moduleObject, it) }

    fun processServerResponse() = processServerResponseMethod?.let { CallVoidMethod(moduleObject, it) }

    private class NopAgentContext : AgentContext {
        override fun get(key: String) = throw NotImplementedError()
        override fun invoke() = throw NotImplementedError()
    }

    private class NopMessageSender : AgentMessageSender {
        override val available
            get() = throw NotImplementedError()
        override fun send(destination: AgentMessageDestination, message: AgentMessage) = throw NotImplementedError()
    }

    private class NopConfiguration : AgentConfiguration {
        override val agentMetadata
            get() = throw NotImplementedError()
        override val parameters
            get() = throw NotImplementedError()
    }

}
