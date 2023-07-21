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
package com.epam.drill.request

import com.epam.drill.jvmapi.callObjectByteArrayMethod
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithBoolean
import com.epam.drill.jvmapi.getObjectMethod
import com.epam.drill.jvmapi.toJByteArray
import com.epam.drill.jvmapi.gen.CallVoidMethod
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

actual object RequestHolder {

    actual fun init(isAsync: Boolean): Unit =
        callObjectVoidMethodWithBoolean(RequestHolder::class, RequestHolder::init, isAsync)

    actual fun store(drillRequest: ByteArray): Unit =
        callRequestHolderStoreMethod(RequestHolder::class, RequestHolder::store, drillRequest)

    actual fun dump(): ByteArray? =
        callObjectByteArrayMethod(RequestHolder::class, RequestHolder::dump)

    actual fun closeSession(): Unit =
        callObjectVoidMethod(RequestHolder::class, RequestHolder::closeSession)

}

private fun callRequestHolderStoreMethod(
    clazz: KClass<out RequestHolder>,
    method: KCallable<Unit>,
    drillRequest: ByteArray
) = getObjectMethod(clazz, method.name, "(Lcom/epam/drill/plugin/DrillRequest;)V").run {
    CallVoidMethod(this.first, this.second, toJByteArray(drillRequest))
}
