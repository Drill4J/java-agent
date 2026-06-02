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

import mu.KotlinLogging
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import kotlinx.serialization.KSerializer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [AgentMessageSender] implementation with [AgentMessageQueue] for storing
 * serialized messages when transport in unavailable state.
 * @see AgentMessageSender
 * @see AgentMessageQueue
 * @see AgentMessageTransport
 */
open class QueuedAgentMessageSender(
    private val transport: AgentMessageTransport,
    private val messageSerializer: AgentMessageSerializer,
    private val destinationMapper: AgentMessageDestinationMapper,
    private val messageQueue: AgentMessageQueue<ByteArray>,
    private val messageSendingListener: MessageSendingListener? = null,
    private val exponentialBackoff: ExponentialBackoff = SimpleExponentialBackoff(),
    maxThreads: Int = 1,
    private val maxRetries: Int = 5
) : AgentMessageSender {
    private val logger = KotlinLogging.logger {}
    private val executor: ExecutorService = Executors.newFixedThreadPool(maxThreads) { runnable ->
        Thread(runnable, "drill-message-sender").apply { isDaemon = true }
    }

    private val isRunning = AtomicBoolean(true)

    init {
        repeat(maxThreads) {
            executor.submit { processQueue() }
        }
    }

    override fun <T> send(destination: AgentMessageDestination, message: T, serializer: KSerializer<T>) {
        val mappedDestination = destinationMapper.map(destination)
        val serializedMessage = messageSerializer.serialize(message, serializer)
        if (!isRunning.get()) {
            handleUnsent(mappedDestination, serializedMessage, "sender is not running")
            return
        }
        if (!messageQueue.offer(Pair(mappedDestination, serializedMessage))) {
            handleUnsent(mappedDestination, serializedMessage, "queue capacity limit reached")
            return
        }
        logger.trace {
            "Queued message to $mappedDestination"
        }
    }

    override fun shutdown() {
        shutdown(Long.MAX_VALUE)
    }

    fun shutdown(flushTimeoutMs: Long) {
        if (!isRunning.compareAndSet(true, false)) return
        logger.info { "Shutting down queued message sender, flush timeout is ${flushTimeoutMs}ms." }
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushTimeoutMs.coerceAtLeast(0))
        executor.shutdown()
        unloadQueue("sender is shutting down", deadlineNanos)
        awaitExecutorDrain(deadlineNanos)
        val remainingMs = remainingMs(deadlineNanos)
        try {
            if (remainingMs > 0 && !executor.awaitTermination(remainingMs, TimeUnit.MILLISECONDS)) {
                logger.warn {
                    "Message sender executor did not terminate in ${remainingMs}ms; " +
                        "leaving worker threads for JVM exit instead of forcing shutdown."
                }
            }
        } catch (e: InterruptedException) {
            logger.warn(e) { "Interrupted while waiting for message sender executor to terminate." }
            Thread.currentThread().interrupt()
        }
        if (messageQueue.size() > 0) {
            logger.warn { "Shutdown completed with ${messageQueue.size()} message(s) still in the queue." }
        }
    }

    private fun awaitExecutorDrain(deadlineNanos: Long) {
        while (messageQueue.size() > 0 && System.nanoTime() < deadlineNanos && !executor.isTerminated) {
            val remainingMs = remainingMs(deadlineNanos).coerceAtLeast(1)
            executor.awaitTermination(minOf(remainingMs, 250), TimeUnit.MILLISECONDS)
        }
    }

    private fun remainingMs(deadlineNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()).coerceAtLeast(0)

    /**
     * Processes the message queue.
     * It will try to send the message from a queue to the destination with exponential backoff.
     */
    private fun processQueue() {
        while (isRunning.get()) {
            val (destination, message) = messageQueue.poll(1, TimeUnit.SECONDS) ?: continue
            runCatching {
                exponentialBackoff.tryWithExponentialBackoff(
                    maxRetries = maxRetries
                ) { attempt, delay ->
                    tryToSend(destination, message, attempt, delay)
                }
            }.onFailure {
                tryToSend(destination, message) || handleUnsent(destination, message, "error occurred: ${it.message}")
            }.onSuccess {
                if (!it) {
                    handleUnsent(destination, message, "attempts exhausted")
                }
            }
        }
    }

    /**
     * Tries to send the message to the destination.
     * @param message The serialized message to send.
     * @param destination The destination to which the message should be sent.
     * @param attempt The current attempt number.
     * @param delay The delay in milliseconds before the next attempt.
     * @return `true` if the message was sent successfully, `false` otherwise.
     */
    private fun tryToSend(
        destination: AgentMessageDestination,
        message: ByteArray,
        attempt: Int = 0,
        delay: Long = 0
    ): Boolean {
        logger.trace {
            "Sending to $destination on attempt: $attempt"
        }
        return transport.send(destination, message, messageSerializer.contentType()).onError { error ->
            logger.trace { "Attempt $attempt send to $destination failed. Retrying in ${delay}ms. Error message: $error" }
        }.onSuccess {
            logger.debug {
                val serializedAsString = message.decodeToString()
                "Sent to $destination on attempt: $attempt, message: $serializedAsString"
            }
            messageSendingListener?.onSent(destination, message)
        }.success
    }

    /**
     * Last attempt to send unsent messages, and register them as unsent if unsuccessful
     */
    private fun unloadQueue(reason: String, deadlineNanos: Long = Long.MAX_VALUE) {
        if (messageQueue.size() == 0) return
        logger.info { "Unloading message queue ($reason), queue size: ${messageQueue.size()}" }
        while (messageQueue.size() > 0) {
            if (System.nanoTime() >= deadlineNanos) {
                logger.warn {
                    "Shutdown flush timeout while unloading queue, ${messageQueue.size()} message(s) remain."
                }
                break
            }
            val queued = messageQueue.poll() ?: break
            val (destination, message) = queued
            tryToSend(destination, message) || handleUnsent(destination, message, reason)
        }
        logger.info { "Finished unloading message queue." }
    }

    /**
     * Handles the case when a message cannot be sent because the queue is full, shutdown, or attempts have been exhausted.
     * @param destination The destination to which the message was intended to be sent.
     * @param message The serialized message that could not be sent.
     * @return `true` if the message was handled, `false` otherwise.
     */
    private fun handleUnsent(
        destination: AgentMessageDestination,
        message: ByteArray,
        reason: String
    ): Boolean = runCatching {
        logger.debug {
            val serializedAsString = message.decodeToString()
            "Failed to send message because $reason, destination: $destination, message: $serializedAsString"
        }
        messageSendingListener?.onUnsent(destination, message)
        true
    }.getOrDefault(false)
}
