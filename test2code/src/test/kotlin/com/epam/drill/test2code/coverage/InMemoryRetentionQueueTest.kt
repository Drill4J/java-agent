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

import java.math.*
import org.junit.jupiter.api.*
import com.epam.drill.plugins.test2code.common.api.*

class InMemoryRetentionQueueTest {

    private val initECD: (Int) -> ExecClassData = { ExecClassData(className = "", probes = Probes(), testId = "") }

    @Test
    fun `queue must not accept elements over size limit`() {
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(Array(6) { List(2, initECD)}.asSequence())
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding elements with size equal to limit should result in queue size exactly at limit`() {
        val sizeLimit = BigInteger.valueOf(10)
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = sizeLimit)

        queue.addAll(Array(1) { List(10, initECD)}.asSequence())
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(sizeLimit.toInt(), actualSizeOfQueue)
    }

    @Test
    fun `adding single element larger than limit should result in an empty queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))

        queue.addAll(Array(1) { List(12, initECD) }.asSequence())

        Assertions.assertTrue(queue.flush().count() == 0)
    }

    @Test
    fun `flush should return the same elements as added to the queue`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val expectedContent = Array(1) { List(5, initECD) }.asSequence()

        queue.addAll(expectedContent)
        val queueContent = queue.flush()

        Assertions.assertEquals(expectedContent.toList(), queueContent.toList())
    }

    @Test
    fun `queue after flush must be empty`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))

        queue.addAll(Array(1) { List(5, initECD) }.asSequence())
        queue.flush()
        val queueContent = queue.flush().toList()

        Assertions.assertTrue(queueContent.isEmpty())
    }

    @Test
    fun `after attempt to add element over limit queue should still accept elements up to limit`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))

        queue.addAll(sequenceOf(
            List(12, initECD),
            List(2, initECD),
            List(2, initECD)
        ))
        val actualSizeOfQueue = queue.flush().sumOf { it.size }

        Assertions.assertEquals(4, actualSizeOfQueue)
    }

    @Test
    fun `queue should contain elements within limit and exclude element exceeding limit `() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        val expectedBytes1 = List(2, initECD)
        val expectedBytes2 = List(2, initECD)
        val expectedBytes3 = List(2, initECD)
        val notExpectedContent = List(12, initECD)

        queue.addAll(sequenceOf(
            expectedBytes1,
            expectedBytes2,
            notExpectedContent,
            expectedBytes3
        ))
        val queueContent = queue.flush().toList()

        Assertions.assertTrue(queueContent.contains(expectedBytes1))
        Assertions.assertTrue(queueContent.contains(expectedBytes2))
        Assertions.assertTrue(queueContent.contains(expectedBytes3))
        Assertions.assertFalse(queueContent.contains(notExpectedContent))
    }

    @Test
    fun `after flush queue must accept new elements`() {
        val queue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))

        queue.addAll(sequenceOf(
            List(2, initECD),
            List(2, initECD)
        ))
        queue.flush()

        val expectedContent = List(2, initECD)
        queue.addAll(sequenceOf(expectedContent))

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

        queue.addAll(sequenceOf(List(1, initECD)))
        val actualSizeOfQueue = queue.flush().count()

        Assertions.assertEquals(0, actualSizeOfQueue)
    }

}
