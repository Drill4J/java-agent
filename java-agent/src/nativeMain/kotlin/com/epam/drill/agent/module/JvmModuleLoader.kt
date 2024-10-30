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

import kotlin.reflect.KClass
import com.epam.drill.agent.common.module.AgentModule
import com.epam.drill.jvmapi.gen.CallObjectMethod
import com.epam.drill.jvmapi.gen.GetObjectClass
import com.epam.drill.jvmapi.gen.NewGlobalRef
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.getObjectMethod
import kotlinx.cinterop.ExperimentalForeignApi

actual object JvmModuleLoader {

    @OptIn(ExperimentalForeignApi::class)
    actual fun loadJvmModule(classname: String): AgentModule =
        callObjectAgentModuleMethodWithString(
            JvmModuleLoader::class,
            JvmModuleLoader::loadJvmModule.name,
            classname
        ).run {
            val moduleClass = NewGlobalRef(GetObjectClass(this))!!
            val moduleRef = NewGlobalRef(this)!!
            InstrumentationAgentModule(classname, moduleClass, moduleRef).also { JvmModuleStorage.add(it) }
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun callObjectAgentModuleMethodWithString(clazz: KClass<out Any>, method: String, string: String?) =
        getObjectMethod(clazz, method, "(Ljava/lang/String;)Lcom/epam/drill/common/agent/module/AgentModule;").run {
            CallObjectMethod(this.first, this.second, string?.let(::NewStringUTF))
        }

}
