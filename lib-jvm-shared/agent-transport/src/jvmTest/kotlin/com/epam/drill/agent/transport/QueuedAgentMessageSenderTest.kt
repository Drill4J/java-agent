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

import kotlin.test.Test
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.ResponseStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.After
import org.junit.Before
import java.lang.Thread.sleep
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class QueuedAgentMessageSenderTest {

    @Serializable
    private class TestAgentMessage(val msg: String) : AgentMessage()

    @MockK
    private lateinit var messageTransport: AgentMessageTransport

    @MockK
    private lateinit var messageSerializer: AgentMessageSerializer

    @MockK
    private lateinit var destinationMapper: AgentMessageDestinationMapper

    @MockK
    private lateinit var messageQueue: AgentMessageQueue<ByteArray>

    @MockK
    private lateinit var messageSendingListener: MessageSendingListener

    private lateinit var sender: QueuedAgentMessageSender

    private val incomingMessage = mutableListOf<TestAgentMessage>()
    private val incomingDestinations = mutableListOf<AgentMessageDestination>()
    private val toSendMessages = mutableListOf<ByteArray>()
    private val toSendContentTypes = mutableListOf<String>()
    private val toSendDestinations = mutableListOf<AgentMessageDestination>()
    private val unsentMessages = mutableListOf<ByteArray>()
    private val unsentDestinations = mutableListOf<AgentMessageDestination>()
    private val sentMessages = mutableListOf<ByteArray>()
    private val sentDestinations = mutableListOf<AgentMessageDestination>()
    private val queueOffers = mutableListOf<Boolean>()
    private val queuePolls = mutableListOf<Pair<AgentMessageDestination, ByteArray>?>()
    private val queue: BlockingQueue<Pair<AgentMessageDestination, ByteArray>> = LinkedBlockingQueue(10)

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() = MockKAnnotations.init(this).also {
        incomingMessage.clear()
        incomingDestinations.clear()
        toSendMessages.clear()
        toSendContentTypes.clear()
        toSendDestinations.clear()
        unsentDestinations.clear()
        unsentMessages.clear()
        sentDestinations.clear()
        sentMessages.clear()
        queueOffers.clear()
        queuePolls.clear()
        queue.clear()

        val serialize: (TestAgentMessage) -> ByteArray = {
            "serialized-${it.msg}".encodeToByteArray()
        }
        val mapDestination: (AgentMessageDestination) -> AgentMessageDestination = {
            AgentMessageDestination("MAP", "mapped-${it.target}")
        }
        every { messageSerializer.serialize(capture(incomingMessage), TestAgentMessage.serializer()) } answers FunctionAnswer {
            serialize(it.invocation.args[0] as TestAgentMessage)
        }
        every { messageSerializer.contentType() } returns "test/test"
        every { destinationMapper.map(capture(incomingDestinations)) } answers FunctionAnswer {
            mapDestination(it.invocation.args[0] as AgentMessageDestination)
        }
        every { messageSendingListener.onUnsent(capture(unsentDestinations), capture(unsentMessages)) } returns Unit
        every { messageSendingListener.onSent(capture(sentDestinations), capture(sentMessages)) } returns Unit
        every { messageQueue.size() } answers FunctionAnswer { queue.size }
        every { messageQueue.poll(any(), any()) } answers FunctionAnswer {
            queue.poll(1, TimeUnit.SECONDS).also(queuePolls::add)
        }
        every { messageQueue.poll() } answers FunctionAnswer {
            queue.poll().also(queuePolls::add)
        }
        every { messageQueue.offer(any()) } answers FunctionAnswer {
            queue.offer(it.invocation.args[0] as Pair<AgentMessageDestination, ByteArray>).also(queueOffers::add)
        }

        sender = QueuedAgentMessageSender(
            messageTransport,
            messageSerializer,
            destinationMapper,
            messageQueue,
            messageSendingListener,
            StubExponentialBackoff()
        )
    }

    @After
    fun shutdown() {
        sender.shutdown()
    }

    @Test
    fun `given ok response, QueuedAgentMessageSender should send messages`() {
        every { messageTransportSending() } returns ResponseStatus(true)

        repeat(10) {
            sender.send(AgentMessageDestination("TYPE", "target-$it"), TestAgentMessage("message-$it"), TestAgentMessage.serializer())
        }

        verifyMethodCalls(calls = 10, sendingAttempts = 10, enqueued = 10, dequeued = 10, sent = 10, unsent = 0)
    }

    @Test
    fun `given bad response, QueuedAgentMessageSender shouldn't send messages`() {
        every { messageTransportSending() } returns ResponseStatus(false)

        repeat(10) {
            sender.send(AgentMessageDestination("TYPE", "target-$it"), TestAgentMessage("message-$it"), TestAgentMessage.serializer())
        }

        verifyMethodCalls(calls = 10, sendingAttempts = 50, enqueued = 10, dequeued = 10, sent = 0, unsent = 10)
    }

    @Test
    fun `given shutdown state, QueuedAgentMessageSender shouldn't add messages to queue`() {
        every { messageTransportSending() } returns ResponseStatus(true)

        sender.shutdown() //shutdown before sending messages
        repeat(10) {
            sender.send(AgentMessageDestination("TYPE", "target-$it"), TestAgentMessage("message-$it"), TestAgentMessage.serializer())
        }

        verifyMethodCalls(calls = 10, sendingAttempts = 0, enqueued = 0, dequeued = 0, sent = 0, unsent = 10)
    }

    @Test
    fun `given limit on queue capacity, QueuedAgentMessageSender shouldn't add messages to queue`() {
        every { messageTransportSending() } returns ResponseStatus(true)
        every { messageQueue.offer(any()) } returns false //queue is full

        repeat(10) {
            sender.send(AgentMessageDestination("TYPE", "target-$it"), TestAgentMessage("message-$it"), TestAgentMessage.serializer())
        }

        verifyMethodCalls(calls = 10, sendingAttempts = 0, enqueued = 0, dequeued = 0, sent = 0, unsent = 10)
    }

    private fun verifyMethodCalls(
        calls: Int? = null,
        sendingAttempts: Int? = null,
        enqueued: Int? = null,
        dequeued: Int? = null,
        sent: Int? = null,
        unsent: Int? = null
    ) {
        calls?.waitFor { verify(exactly = it) { messageSerializer.serialize(any(), TestAgentMessage.serializer()) } }
        calls?.waitFor { verify(exactly = it) { destinationMapper.map(any()) } }
        enqueued?.waitFor {
            assertEquals(it, queueOffers.filter { o -> o }.size)
        }
        dequeued?.waitFor {
            assertEquals(it, queuePolls.filterNotNull().size)
        }

        sendingAttempts?.waitFor { verify(exactly = it) { messageTransport.send(any(), any(), any()) } }
        sent?.waitFor { verify(exactly = it) { messageSendingListener.onSent(any(), any()) } }
        unsent?.waitFor { verify(exactly = it) { messageSendingListener.onUnsent(any(), any()) } }
    }

    private fun <T> T.waitFor(timeout: Long = 1000, block: (T) -> Unit) {
        val start = System.currentTimeMillis()
        val timeIsOut = { System.currentTimeMillis() - start > timeout }
        var error: Throwable? = null
        while (runCatching { block(this) }
                .onFailure { error = it }
                .onSuccess { error = null }
                .isFailure && !timeIsOut()) {
            sleep(10)
        }
        error?.let { throw it }
    }

    private fun MockKMatcherScope.messageTransportSending() = messageTransport.send(
        capture(toSendDestinations),
        capture(toSendMessages),
        capture(toSendContentTypes)
    )

    private class StubExponentialBackoff : ExponentialBackoff {
        override fun tryWithExponentialBackoff(
            initDelay: Long,
            baseDelay: Long,
            maxDelay: Long,
            factor: Double,
            maxRetries: Int,
            onSleep: (Long) -> Unit,
            operation: (Int, Long) -> Boolean
        ): Boolean {
            var attempts = 0
            var result: Boolean
            do {
                result = operation(0, 0)
                attempts++
            } while (attempts < maxRetries && !result)
            return result
        }
    }
}