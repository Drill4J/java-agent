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

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import com.epam.drill.common.agent.Instrumenter
import com.epam.drill.jvmapi.gen.*

class InstrumentationAgentModule(
    pluginId: String,
    pluginApiClass: jclass,
    userPlugin: jobject,
    private val qs: jmethodID? = GetMethodID(pluginApiClass, "instrument", "(Ljava/lang/String;[B)[B")
) : GenericAgentModule(pluginId, pluginApiClass, userPlugin), Instrumenter {

    override fun instrument(className: String, initialBytes: ByteArray) = memScoped<ByteArray?> {
        val classDataLen = initialBytes.size
        val newByteArray: jbyteArray? = NewByteArray(classDataLen)
        initialBytes.usePinned {
            SetByteArrayRegion(newByteArray, 0, classDataLen, it.addressOf(0))
        }
        val bytes = CallObjectMethod(userPlugin, qs, NewStringUTF(className), newByteArray) ?: return null
        val length = GetArrayLength(bytes)
        val buffer: COpaquePointer? = GetPrimitiveArrayCritical(bytes, null)
        try {
            return ByteArray(length).apply {
                usePinned { destination ->
                    platform.posix.memcpy(
                        destination.addressOf(0),
                        buffer,
                        length.convert()
                    )
                }
            }
        } finally { ReleasePrimitiveArrayCritical(bytes, buffer, JNI_ABORT) }
    }

}
