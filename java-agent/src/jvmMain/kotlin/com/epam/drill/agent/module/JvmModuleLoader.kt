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

import com.epam.drill.agent.RequestAgentContext
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.transport.JvmModuleMessageSender
import com.epam.drill.agent.common.AgentContext
import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.module.AgentModule
import com.epam.drill.agent.common.transport.AgentMessageSender

actual object JvmModuleLoader {

    @Suppress("UNCHECKED_CAST")
    actual fun loadJvmModule(classname: String): AgentModule = run {
        val jvmModuleClass = Class.forName(classname) as Class<out AgentModule>
        val constructor = jvmModuleClass.getConstructor(
            String::class.java,
            com.epam.drill.agent.common.AgentContext::class.java,
            AgentMessageSender::class.java,
            AgentConfiguration::class.java
        )
        constructor.newInstance(classname, RequestAgentContext, JvmModuleMessageSender, Configuration)
            .also { JvmModuleStorage.add(it) }
    }

}
