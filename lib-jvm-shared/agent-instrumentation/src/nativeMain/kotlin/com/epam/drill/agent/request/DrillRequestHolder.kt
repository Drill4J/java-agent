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
package com.epam.drill.agent.request

import kotlinx.serialization.protobuf.ProtoBuf
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import com.epam.drill.agent.jvmapi.callObjectByteArrayMethod
import com.epam.drill.agent.jvmapi.callObjectVoidMethod
import com.epam.drill.agent.jvmapi.callObjectVoidMethodWithBoolean
import com.epam.drill.agent.jvmapi.callObjectVoidMethodWithByteArray

actual object DrillRequestHolder : RequestHolder {

    actual override fun remove() =
        callObjectVoidMethod(this::class, RequestHolder::remove)

    actual override fun retrieve() =
        dump()?.let { ProtoBuf.decodeFromByteArray(DrillRequest.serializer(), it) }

    actual override fun store(drillRequest: DrillRequest) =
        store(ProtoBuf.encodeToByteArray(DrillRequest.serializer(), drillRequest))

    actual fun store(drillRequest: ByteArray) =
        callObjectVoidMethodWithByteArray(this::class, RequestHolder::store, drillRequest)

    actual fun dump(): ByteArray? =
        callObjectByteArrayMethod(this::class, this::dump)
}