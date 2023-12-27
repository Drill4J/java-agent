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
package com.epam.drill.agent

import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.AgentModule
import com.epam.drill.common.agent.AgentContext
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.test2code.Test2Code

actual object JvmModuleLoader {

    actual fun loadJvmModule(id: String): AgentModule<*> = run {
        val jvmModuleClass = getJvmModuleClass(id)!!
        val constructor = jvmModuleClass.getConstructor(String::class.java, AgentContext::class.java, AgentMessageSender::class.java)
        constructor.newInstance(id, RequestHolder.agentContext, JvmModuleMessageSender).also { JvmModuleStorage.add(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getJvmModuleClass(id: String): Class<AgentModule<*>>? = when(id) {
        "test2code" -> Test2Code::class.java as Class<AgentModule<*>>
        else -> null
    }

}
