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
package com.epam.drill.agent.lifecycle

import com.epam.drill.agent.common.lifecycle.AgentShutdownRegistry
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.transport.DataIngestMessageSender
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates JVM shutdown: stop producers, then flush the shared message sender.
 */
actual object AgentShutdownCoordinator {
    private val logger = KotlinLogging.logger {}
    private val hookInstalled = AtomicBoolean(false)
    private val shutdownCompleted = AtomicBoolean(false)

    actual fun install() {
        if (!hookInstalled.compareAndSet(false, true)) return
        Runtime.getRuntime().addShutdownHook(
            Thread({ shutdown() }, "drill-shutdown-hook")
        )
        logger.debug { "Agent shutdown hook installed." }
    }

    actual fun shutdown() {
        if (!shutdownCompleted.compareAndSet(false, true)) return
        runShutdown()
    }

    private fun runShutdown() {
        val flushTimeoutMs = Configuration.parameters[ParameterDefinitions.SHUTDOWN_FLUSH_TIMEOUT_MS].toLong()
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushTimeoutMs)
        logger.debug { "Agent shutdown started, flush timeout is ${flushTimeoutMs}ms." }

        for (task in AgentShutdownRegistry.tasks()) {
            val remainingMs = remainingMs(deadlineNanos)
            if (remainingMs <= 0) {
                logger.warn { "Shutdown flush timeout reached before task '${task.name}'." }
                break
            }
            runCatching {
                logger.debug { "Running shutdown task '${task.name}' (${remainingMs}ms remaining)." }
                task.action(remainingMs)
            }.onFailure {
                logger.error(it) { "Shutdown task '${task.name}' failed." }
            }
        }

        val remainingMs = remainingMs(deadlineNanos)
        if (remainingMs <= 0) {
            logger.warn { "Shutdown flush timeout exhausted, attempting a best-effort message sender flush." }
        } else {
            logger.debug { "Flushing message sender, ${remainingMs}ms remaining." }
        }
        runCatching {
            DataIngestMessageSender.shutdownWithTimeout(remainingMs)
        }.onFailure {
            logger.error(it) { "Message sender shutdown failed." }
        }
        logger.debug { "Agent shutdown completed." }
    }

    private fun remainingMs(deadlineNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()).coerceAtLeast(0)
}
