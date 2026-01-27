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
package com.epam.drill.agent.test.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*

class PropertyDecoder(val map: Map<String, Any>) : NamedValueDecoder() {
    private var currentIndex = 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeTaggedInt(nested("size"))
    }

    override fun decodeTaggedValue(tag: String): Any {
        return map.getValue(tag)
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return map.getValue(tag) as Boolean
    }

    override fun decodeTaggedLong(tag: String): Long {
        return map.getValue(tag) as Long
    }

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
        return (map.getValue(tag) as Enum<*>).ordinal
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T {
        return map[this.currentTagOrNull] as? T ?: super.decodeSerializableValue(deserializer, previousValue)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val tag = nested("size")
        val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
        while (currentIndex < size) {
            val name = descriptor.getTag(currentIndex++)
            if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
        }
        return CompositeDecoder.DECODE_DONE
    }
}
