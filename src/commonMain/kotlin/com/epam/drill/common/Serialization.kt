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
package com.epam.drill.common

import kotlinx.serialization.*

internal fun <T> BinaryFormat.dump(
    serializer: SerializationStrategy<T>,
    value: T
): ByteArray = encodeToByteArray(serializer, value)

internal fun <T> BinaryFormat.load(
    deserializer: DeserializationStrategy<T>,
    bytes: ByteArray
): T = decodeFromByteArray(deserializer, bytes)
