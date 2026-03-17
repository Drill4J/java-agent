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
package com.epam.drill.agent.test2code.common.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.BooleanArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

expect class Probes(size: Int) {
    fun length(): Int
    fun get(ind: Int): Boolean
    fun set(ind: Int, value: Boolean)
}

object BitSetSerializer : KSerializer<Probes> {

    override fun serialize(encoder: Encoder, value: Probes) {
        encoder.encodeSerializableValue(BooleanArraySerializer(), value.toBooleanArray())
    }

    override fun deserialize(decoder: Decoder): Probes {
        val decodeSerializableValue = decoder.decodeSerializableValue(BooleanArraySerializer())
        return decodeSerializableValue.toBitSet()
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("BitSet")
}

/**
 * "Probes" is a java.util.BitSet alias
 * it also extended with _custom_ creator functions
 * see "why" bellow:
 *
 * Problem:
 *    converting boolean array to BitSet presents the issue - we loose array length property
 *
 * Illustration:
 *          Given boolean array [false, true, false, false]
 *          Expect converted    0100                               // intuitively expected result
 *          But actually get    0100000000000000000000000000000... // zero-padded until end of Long (64 bits)
 *
 * Explanation:
 *          - BitSet is backed by Long-s, allocated "as needed", each new long adding 64 bits
 *          - Array.length() returns _actual_ number of elements in array
 *          - BitSet.length() returns index of last set (set to 1) bit
 *          - BitSet.size     returns size of backing Long fields in bits
 *          - even if the actual number of probes is 65, BitSet.size() will return 128
 *
 * Our workaround:
 *          When converting boolean array to BitSet
 *          get originalArray.length()
 *          set the bit on corresponding index to 1
 *          result - BitSet.length() will return that bit index
 *
 * TL;DR Do not use ordinary BitSet methods to convert Probes; Use only ones implemented bellow
 *
 */
fun Probes.toBooleanArray(): BooleanArray {
    return BooleanArray(length() - 1) {// -1 drops end-of-original-array-indicator bit
        get(it)
    }
}

fun Probes.toList(): List<Boolean> {
    return toBooleanArray().toList()
}

fun BooleanArray.toBitSet(): Probes {
    val finalSize = size + 1
    return Probes(finalSize).apply {
        forEachIndexed { index, b ->
            set(index, b)
        }
        set(size, true) // set end-of-original-array-indicator bit (see explanation above)
    }
}

fun List<Boolean>.toBitSet(): Probes {
    val finalSize = size + 1
    return Probes(finalSize).apply {
        forEachIndexed { index, b ->
            set(index, b)
        }
        set(size, true) // set end-of-original-array-indicator bit (see explanation above)
    }
}

fun probesOf(vararg elements: Boolean): Probes {
    return elements.toBitSet()
}
