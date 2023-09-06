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

import org.junit.jupiter.api.*
import java.math.*

class InMemoryRetentionQueueTest {

    @Test
    fun `adding multiple elements exceeding limit should maintain total size at limit`() {
        val byteCount = 6
        val innerArraySize = 2
        val bytes = Array(byteCount) { ByteArray(innerArraySize) { 1 } }.asSequence()
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(bytes)

        val actualSizeOfQueue = queue.flush().sumOf { it.size }
        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding elements with size equal to limit should result in queue size exactly at limit`() {
        val byteCount = 1
        val innerArraySize = 10
        val bytes = Array(byteCount) { ByteArray(innerArraySize) { 1 } }.asSequence()
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(bytes)

        val actualSizeOfQueue = queue.flush().sumOf { it.size }
        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding single element larger than limit should result in an empty queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val byteCount = 1
        val innerArraySize = 12
        val bytes = Array(byteCount) { ByteArray(innerArraySize) { 1 } }.asSequence()

        queue.addAll(bytes)

        Assertions.assertTrue(queue.flush().count() == 0)
    }

    @Test
    fun `flush should return the same elements as added to the queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val byteCount = 1
        val innerArraySize = 5
        val bytes = Array(byteCount) { ByteArray(innerArraySize) { 1 } }.asSequence()

        queue.addAll(bytes)
        val actualBytes = queue.flush()

        Assertions.assertEquals(bytes.toList(), actualBytes.toList())
    }

    @Test
    fun `queue after flush must be empty`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val byteCount = 1
        val innerArraySize = 5
        val bytes = Array(byteCount) { ByteArray(innerArraySize) { 1 } }.asSequence()

        queue.addAll(bytes)

        val bytes1 = queue.flush().toList()
        val bytes2 = queue.flush().toList()

        Assertions.assertTrue(bytes2.isEmpty())
    }

    @Test
    fun `queue should only fit elements with size less than or equal to total limit`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val expectedSizeOfQueue = 4
        val bytes = sequenceOf(
            ByteArray(12) { 1 },
            ByteArray(2) { 1 },
            ByteArray(2) { 1 }
        )

        queue.addAll(bytes)
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(expectedSizeOfQueue, actualSizeOfQueue)
    }

    @Test
    fun `queue should contain elements within limit and exclude element exceeding limit `() {
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)
        val expectedBytes1 = ByteArray(2) { 1 }
        val expectedBytes2 = ByteArray(2) { 2 }
        val expectedBytes3 = ByteArray(2) { 3 }
        val notExpectedBytes = ByteArray(12) { 4 }
        val bytes = sequenceOf(
            expectedBytes1,
            expectedBytes2,
            notExpectedBytes,
            expectedBytes3
        )

        queue.addAll(bytes)

        val actualSizeOfQueue = queue.flush().toList()

        Assertions.assertTrue(actualSizeOfQueue.contains(expectedBytes1))
        Assertions.assertTrue(actualSizeOfQueue.contains(expectedBytes2))
        Assertions.assertTrue(actualSizeOfQueue.contains(expectedBytes3))
        Assertions.assertFalse(actualSizeOfQueue.contains(notExpectedBytes))
    }

    @Test
    fun `adding new elements after flush should be reflected in queue content`() {
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)
        val expectedBytes1 = ByteArray(2) { 1 }
        val expectedBytes2 = ByteArray(2) { 2 }
        var bytes = sequenceOf(
            expectedBytes1,
            expectedBytes2
        )

        queue.addAll(bytes)
        val bytesFromQueue1 = queue.flush().toList()

        val expectedBytes3 = ByteArray(2) { 3 }
        bytes = sequenceOf(expectedBytes3)
        queue.addAll(bytes)

        val bytesFromQueue2 = queue.flush().toList()

        Assertions.assertTrue(bytesFromQueue2.contains(expectedBytes3))
    }

    @Test
    fun `initializing queue with negative totalSizeByteLimit should throw exception`() {
        val sizeLimit = BigInteger.valueOf(-10)

        assertThrows<IllegalArgumentException> {
            val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)
        }
    }

    @Test
    fun `adding elements to a queue with zero totalSizeByteLimit should return an empty queue`() {
        val expectedSizeOfQueue = 0
        val sizeLimit = BigInteger.ZERO
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)
        val bytes = sequenceOf(ByteArray(1) { 1 })

        queue.addAll(bytes)

        val actualSizeOfQueue = queue.flush().count()

        Assertions.assertEquals(expectedSizeOfQueue, actualSizeOfQueue)
    }

}
