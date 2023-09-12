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
import java.math.*
import java.util.*
import java.util.concurrent.*


interface RetentionQueue {
    fun addAll(data: Sequence<ByteArray>)
    fun flush(): Sequence<ByteArray>
}

class InMemoryRetentionQueue(
    private var queue: Queue<ByteArray> = ConcurrentLinkedQueue(),
    private val totalSizeByteLimit: BigInteger,
) : RetentionQueue {

    init {
        require(totalSizeByteLimit >= BigInteger.ZERO) {
            "Total size byte limit must be a positive value"
        }
    }

    private val logger = KotlinLogging.logger("com.epam.drill.test2code.coverage.InMemoryRetentionQueue")
    private var totalBytes: BigInteger = BigInteger.ZERO

    override fun addAll(data: Sequence<ByteArray>) {
        data.forEach { bytes ->
            if (totalBytes.plus(BigInteger.valueOf(bytes.size.toLong())) > totalSizeByteLimit) {
                logger.info { "Cannot add element of size ${bytes.size}. Current usage ${totalBytes}/${totalSizeByteLimit} bytes" }
                return@forEach
            }
            val added = queue.offer(bytes)
            if (!added) {
                logger.info { "Cannot add to queue: ${bytes.size}" }
                return@forEach
            }
            totalBytes += BigInteger.valueOf(bytes.size.toLong())
        }
    }

    override fun flush(): Sequence<ByteArray> {
        val bytes = queue.toList().asSequence()
        queue.clear()
        totalBytes = BigInteger.ZERO
        return bytes
    }
}