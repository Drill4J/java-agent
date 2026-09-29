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

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface TestSessionHeartbeatSender {
    fun startSendingHeartbeat()
    fun stopSendingHeartbeat(remainingMs: Long)
}

/**
 * Periodically reports the test session heartbeat status to the Backend.
 *
 * While the session is active it sends [TestSessionStatus.RUNNING] on a fixed
 * interval. On graceful shutdown it stops the scheduler and sends a final
 * [TestSessionStatus.FINISHED] status. All requests are sent synchronously
 * through the provided [sender] (a DIRECT sender, never the queued pipeline).
 */
class IntervalTestSessionHeartbeatSender(
    private val sender: AgentMessageSender,
    private val intervalMs: Long,
    private val groupId: String,
    private val testProjectId: String,
    private val testSessionId: String,
) : TestSessionHeartbeatSender {
    private val logger = KotlinLogging.logger {}
    private val destination = AgentMessageDestination("PUT", "sessions/heartbeat")
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "drill-session-heartbeat-sender").apply { isDaemon = true }
    }

    override fun startSendingHeartbeat() {
        scheduledThreadPool.scheduleAtFixedRate(
            {
                try {
                    sendStatus(TestSessionStatus.RUNNING)
                } catch (t: Throwable) {
                    logger.error(t) { "Test session heartbeat status sending job failed" }
                }
            },
            intervalMs,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.info { "Test session heartbeat status sending job is started." }
    }

    override fun stopSendingHeartbeat(remainingMs: Long) {
        scheduledThreadPool.shutdown()
        if (remainingMs > 0 && !scheduledThreadPool.awaitTermination(remainingMs, TimeUnit.MILLISECONDS)) {
            logger.warn { "Test session heartbeat sending scheduler did not stop within ${remainingMs}ms; leaving it for JVM exit." }
        }
        try {
            sendStatus(TestSessionStatus.FINISHED)
        } catch (t: Throwable) {
            logger.error(t) { "Failed to send SHUTDOWN test session heartbeat status" }
        }
        logger.info { "Test session heartbeat status sending job is stopped." }
    }

    private fun sendStatus(status: TestSessionStatus) {
        sender.send(
            destination,
            TestSessionHeartbeatPayload(
                groupId = groupId,
                testProjectId = testProjectId,
                testSessionId = testSessionId,
                status = status
            ),
            TestSessionHeartbeatPayload.serializer()
        )
    }
}
