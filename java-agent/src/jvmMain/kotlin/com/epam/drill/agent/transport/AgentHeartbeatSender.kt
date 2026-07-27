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

interface AgentHeartbeatSender {
    fun startSendingHeartbeat()
    fun stopSendingHeartbeat(remainingMs: Long)
}

/**
 * Periodically reports the agent heartbeat status to the Backend.
 *
 * While the agent is running it sends [AgentHeartbeatStatus.RUNNING] on a fixed
 * interval. On graceful shutdown it stops the scheduler and sends a final
 * [AgentHeartbeatStatus.SHUTDOWN] status. All requests are sent synchronously
 * through the provided [sender] (a DIRECT sender, never the queued pipeline).
 */
class IntervalAgentHeartbeatSender(
    private val sender: AgentMessageSender,
    private val intervalMs: Long,
    private val groupId: String,
    private val appId: String,
    private val instanceId: String,
) : AgentHeartbeatSender {
    private val logger = KotlinLogging.logger {}
    private val destination = AgentMessageDestination("PUT", "instances/heartbeat")
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "drill-heartbeat-sender").apply { isDaemon = true }
    }

    override fun startSendingHeartbeat() {
        scheduledThreadPool.scheduleAtFixedRate(
            {
                try {
                    sendStatus(AgentHeartbeatStatus.RUNNING)
                } catch (t: Throwable) {
                    logger.error(t) { "Heartbeat status sending job failed" }
                }
            },
            intervalMs,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.info { "Heartbeat status sending job is started." }
    }

    override fun stopSendingHeartbeat(remainingMs: Long) {
        scheduledThreadPool.shutdown()
        if (remainingMs > 0 && !scheduledThreadPool.awaitTermination(remainingMs, TimeUnit.MILLISECONDS)) {
            logger.warn { "Heartbeat sending scheduler did not stop within ${remainingMs}ms; leaving it for JVM exit." }
        }
        try {
            sendStatus(AgentHeartbeatStatus.SHUTDOWN)
        } catch (t: Throwable) {
            logger.error(t) { "Failed to send SHUTDOWN heartbeat status" }
        }
        logger.info { "Heartbeat status sending job is stopped." }
    }

    private fun sendStatus(status: AgentHeartbeatStatus) {
        sender.send(
            destination,
            AgentHeartbeatPayload(
                groupId = groupId,
                appId = appId,
                instanceId = instanceId,
                status = status
            ),
            AgentHeartbeatPayload.serializer()
        )
    }
}
