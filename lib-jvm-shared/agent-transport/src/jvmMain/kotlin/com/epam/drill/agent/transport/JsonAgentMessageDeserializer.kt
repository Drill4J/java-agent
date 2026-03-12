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
package com.epam.drill.agent.transport

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import kotlin.reflect.KClass

class JsonAgentMessageDeserializer: AgentMessageDeserializer {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun contentType(): String = "application/json"

    @OptIn(InternalSerializationApi::class)
    override fun <T: Any> deserialize(message: ByteArray, clazz: KClass<T>): T = ByteArrayInputStream(message).use {
        json.decodeFromStream(clazz.serializer(), it)
    }
}