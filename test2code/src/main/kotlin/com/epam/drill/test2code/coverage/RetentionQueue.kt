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
import java.util.concurrent.atomic.*


interface RetentionQueue {
    fun addAll(data: Sequence<ByteArray>)
    fun flush(): Sequence<ByteArray>
}

class InMemoryRetentionQueue(
    private var queue: Queue<ByteArray> = ConcurrentLinkedQueue(),
    private val totalSizeByteLimit: Long,
) : RetentionQueue {
    private val logger = KotlinLogging.logger("com.epam.drill.test2code.coverage.InMemoryRetentionQueue")
    private val sizeOfQueue: AtomicLong = AtomicLong(0)

    override fun addAll(data: Sequence<ByteArray>) {
        data.filter { it.size < totalSizeByteLimit }.forEach { bytes ->
            if (sizeOfQueue.get() >= totalSizeByteLimit) {
                logger.info { "InMemoryRetentionQueue is full. Cannot add to queue, count of bytes: ${bytes.size}." }
                return@forEach
            }
            val isOffer = queue.offer(bytes)
            if (!isOffer) {
                logger.info { "Cannot add to queue: ${bytes.size}" }
                return@forEach
            }
            sizeOfQueue.set(bytes.size.toLong())
        }
    }

    override fun flush(): Sequence<ByteArray> {
        return sequence {
            for (item in queue) {
                yield(item)
            }
            queue.clear()
            sizeOfQueue.set(0)
        }
    }
}