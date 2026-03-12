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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination

class InMemoryAgentMessageQueueTest {

    private lateinit var messageDestination: AgentMessageDestination

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        messageDestination = AgentMessageDestination("foo", "bar")
    }

    @Test
    fun `add to non-full`() {
        val queue = InMemoryAgentMessageQueue(1100)
        for(i in 1..10) queue.add(Pair(messageDestination, "somestring".encodeToByteArray()))

        assertEquals(10, queue.size())
        assertEquals((6 + 10) * 10, queue.bytesSize())
    }

    @Test
    fun `offer to non-full`() {
        val queue = InMemoryAgentMessageQueue(1100)
        for(i in 1..10) queue.offer(Pair(messageDestination, "somestring".encodeToByteArray()))

        assertEquals(10, queue.size())
        assertEquals((6 + 10)  * 10, queue.bytesSize())
    }

    @Test
    fun `remove from non-empty`() {
        val queue = InMemoryAgentMessageQueue(1100)
        for(i in 0..9) queue.offer(Pair(messageDestination, "somestring$i".encodeToByteArray()))

        val removed1 = queue.remove()
        verifyQueueElement(queue, 9, removed1, "somestring0")

        val removed2 = queue.remove()
        verifyQueueElement(queue, 8, removed2, "somestring1")
    }

    @Test
    fun `poll from non-empty`() {
        val queue = InMemoryAgentMessageQueue( 1100)
        for(i in 0..9) queue.offer(Pair(messageDestination, "somestring$i".encodeToByteArray()))

        val polled1 = queue.poll()
        verifyQueueElement(queue, 9, polled1, "somestring0")

        val polled2 = queue.poll()
        verifyQueueElement(queue, 8, polled2, "somestring1")
    }

    @Test
    fun `peek from non-empty`() {
        val queue = InMemoryAgentMessageQueue(1100)
        for(i in 0..9) queue.offer(Pair(messageDestination, "somestring$i".encodeToByteArray()))

        val element1 = queue.peek()
        verifyQueueElement(queue, 10, element1, "somestring0")

        val element2 = queue.peek()
        verifyQueueElement(queue, 10, element2, "somestring0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add to full`() {
        val queue = InMemoryAgentMessageQueue(140)
        for(i in 0..8) queue.add(Pair(messageDestination, "somestring".encodeToByteArray()))

        assertEquals(9, queue.size())
        assertEquals((6 + 10) * 9, queue.bytesSize())

        queue.add(Pair(messageDestination, "somestring".encodeToByteArray()))
    }

    @Test
    fun `offer to full`() {
        val queue = InMemoryAgentMessageQueue(150)
        for(i in 0..9) queue.offer(Pair(messageDestination, "somestring".encodeToByteArray()))

        assertEquals(9, queue.size())
        assertEquals((6 + 10) * 9, queue.bytesSize())
    }

    @Test(expected = NoSuchElementException::class)
    fun `remove from empty`() {
        val queue = InMemoryAgentMessageQueue(1100)
        queue.remove()
    }

    @Test
    fun `poll from empty`() {
        val queue = InMemoryAgentMessageQueue(1100)
        assertNull(queue.poll())
    }

    @Test
    fun `peek from empty`() {
        val queue = InMemoryAgentMessageQueue(1100)
        assertNull(queue.peek())
    }

    private fun verifyQueueElement(
        queue: InMemoryAgentMessageQueue,
        size: Int,
        element: Pair<AgentMessageDestination, ByteArray>?,
        value: String
    ) {
        assertEquals(size, queue.size())
        assertEquals(size * (6 + 11L), queue.bytesSize())
        assertEquals(messageDestination, element?.first)
        assertEquals(value, element?.second?.decodeToString())
    }

}
