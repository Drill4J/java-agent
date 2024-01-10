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

import kotlinx.cinterop.memScoped
import com.epam.drill.common.agent.Instrumenter
import com.epam.drill.jvmapi.gen.CallObjectMethod
import com.epam.drill.jvmapi.gen.GetMethodID
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.gen.jclass
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.toByteArray
import com.epam.drill.jvmapi.toJByteArray

class InstrumentationAgentModule(
    moduleId: String,
    moduleClass: jclass,
    moduleObject: jobject
) : GenericAgentModule(moduleId, moduleClass, moduleObject), Instrumenter {

    private val instrumentMethod = GetMethodID(moduleClass, Instrumenter::instrument.name, "(Ljava/lang/String;[B)[B")

    override fun instrument(className: String, initialBytes: ByteArray) = memScoped {
        CallObjectMethod(
            moduleObject,
            instrumentMethod,
            NewStringUTF(className),
            toJByteArray(initialBytes)
        )?.let(::toByteArray)
    }

}
