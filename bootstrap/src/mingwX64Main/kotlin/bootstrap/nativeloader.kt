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
package com.epam.drill.bootstrap

import kotlinx.cinterop.*
import platform.windows.*


actual fun agentLoad(path: String): Any? = memScoped {
    LoadLibrary!!(path.replace("/", "\\").toLPCWSTR(this).pointed.ptr)
        ?.let { hModule -> GetProcAddress(hModule, "Agent_OnLoad") }
}

private fun String.toLPCWSTR(ms: MemScope): CArrayPointer<UShortVar> {
    val length = this.length
    val allocArray = ms.allocArray<UShortVar>(length.toLong())
    for (i in 0 until length) {
        allocArray[i] = this[i].code.toShort().toUShort()
    }
    return allocArray
}
