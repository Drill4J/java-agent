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
    fun `queue must not accept elements over size limit`() {
        val elements = Array(6) { ByteArray(2) { 1 } }.asSequence()
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(elements)
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding elements with size equal to limit should result in queue size exactly at limit`() {
        val elements = Array(1) { ByteArray(10) { 1 } }.asSequence()
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(elements)
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding single element larger than limit should result in an empty queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val elements = Array(1) { ByteArray(12) { 1 } }.asSequence()

        queue.addAll(elements)

        Assertions.assertTrue(queue.flush().count() == 0)
    }

    @Test
    fun `flush should return the same elements as added to the queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val elements = Array(1) { ByteArray(5) { 1 } }.asSequence()

        queue.addAll(elements)
        val queueContent = queue.flush()

        Assertions.assertEquals(elements.toList(), queueContent.toList())
    }

    @Test
    fun `queue after flush must be empty`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val elements = Array(1) { ByteArray(5) { 1 } }.asSequence()

        queue.addAll(elements)
        queue.flush()
        val queueContent = queue.flush().toList()

        Assertions.assertTrue(queueContent.isEmpty())
    }

    @Test
    fun `after attempt to add element over limit queue should still accept elements up to limit`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val elements = sequenceOf(
            ByteArray(12) { 1 },
            ByteArray(2) { 1 },
            ByteArray(2) { 1 }
        )

        queue.addAll(elements)
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(4, actualSizeOfQueue)
    }

    @Test
    fun `queue should contain elements within limit and exclude element exceeding limit `() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val expectedBytes1 = ByteArray(2) { 1 }
        val expectedBytes2 = ByteArray(2) { 2 }
        val expectedBytes3 = ByteArray(2) { 3 }
        val notExpectedBytes = ByteArray(12) { 4 }
        val elements = sequenceOf(
            expectedBytes1,
            expectedBytes2,
            notExpectedBytes,
            expectedBytes3
        )

        queue.addAll(elements)
        val queueContent = queue.flush().toList()

        Assertions.assertTrue(queueContent.contains(expectedBytes1))
        Assertions.assertTrue(queueContent.contains(expectedBytes2))
        Assertions.assertTrue(queueContent.contains(expectedBytes3))
        Assertions.assertFalse(queueContent.contains(notExpectedBytes))
    }

    @Test
    fun `after flush queue must accept new elements`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        var elements = sequenceOf(
            ByteArray(2) { 1 },
            ByteArray(2) { 2 }
        )

        queue.addAll(elements)
        queue.flush()

        val expectedContent = ByteArray(2) { 3 }
        elements = sequenceOf(expectedContent)
        queue.addAll(elements)

        val queueContent = queue.flush().toList()

        Assertions.assertTrue(queueContent.contains(expectedContent))
    }

    @Test
    fun `initializing queue with negative totalSizeByteLimit should throw exception`() {
        assertThrows<IllegalArgumentException> {
            InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(-10))
        }
    }

    @Test
    fun `adding elements to a queue with zero totalSizeByteLimit should return an empty queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.ZERO)

        queue.addAll(sequenceOf(ByteArray(1) { 1 }))
        val actualSizeOfQueue = queue.flush().count()

        Assertions.assertEquals(0, actualSizeOfQueue)
    }

}
