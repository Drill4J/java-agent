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
package com.epam.drill.test2code.coverage

import mu.*
import java.util.*
import java.util.concurrent.*


interface Buffer<T> {
    fun addAll(data: Sequence<ByteArray>)
    fun flush(): Sequence<T>
}

class InMemoryBuffer(
    private var buffer: Queue<ByteArray> = ConcurrentLinkedQueue(),
) : Buffer<ByteArray> {
    private val logger = KotlinLogging.logger {}

    override fun addAll(data: Sequence<ByteArray>) {
        if (buffer.sumOf { it.size } >= 2000) {
            logger.info { "Cannot add to buffer. Buffer is full" }
            return
        }
        data.forEach {
            buffer.offer(it)
        }
    }

    override fun flush(): Sequence<ByteArray> {
//        return ConcurrentLinkedQueue(buffer).asSequence().also { buffer.clear() }
        return sequence {
            for (item in buffer) {
                yield(item)
            }
            buffer.clear()
        }
    }
}