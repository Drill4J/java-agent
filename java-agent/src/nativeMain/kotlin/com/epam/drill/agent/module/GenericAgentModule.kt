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

import com.epam.drill.agent.common.AgentContext
import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.module.AgentModule
import com.epam.drill.agent.common.request.RequestProcessor
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import com.epam.drill.agent.jvmapi.gen.CallVoidMethod
import com.epam.drill.agent.jvmapi.gen.GetMethodID
import com.epam.drill.agent.jvmapi.gen.jclass
import com.epam.drill.agent.jvmapi.gen.jobject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.KSerializer

@OptIn(ExperimentalForeignApi::class)
open class GenericAgentModule(
    moduleId: String,
    moduleClass: jclass,
    protected val moduleObject: jobject
) : AgentModule(
    moduleId,
    NopAgentContext(),
    NopMessageSender(),
    NopConfiguration()
), RequestProcessor {

    private val loadMethod = GetMethodID(moduleClass, AgentModule::load.name, "()V")
    private val onConnectMethod = GetMethodID(moduleClass, AgentModule::onConnect.name, "()V")
    private val processServerRequestMethod = GetMethodID(moduleClass, RequestProcessor::processServerRequest.name, "()V")
    private val processServerResponseMethod = GetMethodID(moduleClass, RequestProcessor::processServerResponse.name, "()V")

    override fun load() = CallVoidMethod(moduleObject, loadMethod)

    override fun onConnect() = CallVoidMethod(moduleObject, onConnectMethod)

    override fun processServerRequest() = processServerRequestMethod?.let { CallVoidMethod(moduleObject, it) } ?: Unit

    override fun processServerResponse() = processServerResponseMethod?.let { CallVoidMethod(moduleObject, it) } ?: Unit

    private class NopAgentContext : com.epam.drill.agent.common.AgentContext {
        override fun get(key: String) = throw NotImplementedError()
        override fun invoke() = throw NotImplementedError()
    }

    private class NopMessageSender : AgentMessageSender {
        override fun <T> send(destination: AgentMessageDestination, message: T, serializer: KSerializer<T>) = throw NotImplementedError()
    }

    private class NopConfiguration : AgentConfiguration {
        override val agentMetadata
            get() = throw NotImplementedError()
        override val parameters
            get() = throw NotImplementedError()
    }

}
