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
package com.epam.drill.agent.websocket

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import com.epam.drill.jvmapi.gen.CallVoidMethod
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.getObjectMethod

object WsClient {

    fun connect(adminUrl: String, async: Boolean = false): Unit =
        callObjectVoidMethodWithStringAndBoolean(WsClient::class, WsClient::connect, adminUrl, async)

}

private fun callObjectVoidMethodWithStringAndBoolean(clazz: KClass<out Any>, method: String, string: String?, bool: Boolean) =
    getObjectMethod(clazz, method, "(Ljava/lang/String;Z)V").run {
        CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF), bool)
    }

private fun callObjectVoidMethodWithStringAndBoolean(clazz: KClass<out Any>, method: KCallable<Unit>, string: String?, bool: Boolean) =
    callObjectVoidMethodWithStringAndBoolean(clazz, method.name, string, bool)
