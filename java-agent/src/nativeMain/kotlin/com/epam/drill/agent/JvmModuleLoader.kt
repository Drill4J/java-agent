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

import com.epam.drill.agent.jvm.callObjectAgentModuleMethodWithString
import com.epam.drill.agent.module.InstrumentationAgentModule
import com.epam.drill.common.agent.AgentModule
import com.epam.drill.jvmapi.gen.GetObjectClass
import com.epam.drill.jvmapi.gen.NewGlobalRef

actual object JvmModuleLoader {

    actual fun loadJvmModule(id: String): AgentModule<*> =
        callObjectAgentModuleMethodWithString(JvmModuleLoader::class, JvmModuleLoader::loadJvmModule, id).run {
            val moduleClass = NewGlobalRef(GetObjectClass(this))!!
            val moduleRef = NewGlobalRef(this)!!
            InstrumentationAgentModule(id, moduleClass, moduleRef).also { PluginStorage.add(it) }
        }

}
