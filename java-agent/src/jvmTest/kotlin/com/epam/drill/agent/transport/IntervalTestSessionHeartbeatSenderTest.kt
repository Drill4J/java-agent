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

import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import kotlinx.serialization.KSerializer

class IntervalTestSessionHeartbeatSenderTest {

    private class RecordingSender : AgentMessageSender {
        val sent = Collections.synchronizedList(mutableListOf<Pair<AgentMessageDestination, Any?>>())
        override fun <T> send(destination: AgentMessageDestination, message: T, serializer: KSerializer<T>) {
            sent.add(destination to message)
        }
    }

    private fun newSender(recorder: RecordingSender, intervalMs: Long = 50L) = IntervalTestSessionHeartbeatSender(
        sender = recorder,
        intervalMs = intervalMs,
        groupId = "someGroupId",
        testProjectId = "someTestProjectId",
        testSessionId = "someTestSessionId"
    )

    @Test
    fun `should periodically send RUNNING status to sessions heartbeat`() {
        val recorder = RecordingSender()
        val sender = newSender(recorder, intervalMs = 50L)

        sender.startSendingHeartbeat()
        Thread.sleep(200)
        sender.stopSendingHeartbeat(1000)

        val running = recorder.sent.map { it.second }.filterIsInstance<TestSessionHeartbeatPayload>()
            .filter { it.status == TestSessionStatus.RUNNING }
        assertTrue(running.size >= 2, "Expected multiple RUNNING statuses, got ${running.size}")

        val destination = recorder.sent.first().first
        assertEquals("PUT", destination.type)
        assertEquals("sessions/heartbeat", destination.target)

        val payload = running.first()
        assertEquals("someGroupId", payload.groupId)
        assertEquals("someTestProjectId", payload.testProjectId)
        assertEquals("someTestSessionId", payload.testSessionId)
    }

    @Test
    fun `should send SHUTDOWN status on stop`() {
        val recorder = RecordingSender()
        val sender = newSender(recorder, intervalMs = 10_000L)

        sender.startSendingHeartbeat()
        sender.stopSendingHeartbeat(1000)

        val statuses = recorder.sent.map { it.second }.filterIsInstance<TestSessionHeartbeatPayload>().map { it.status }
        assertEquals(TestSessionStatus.FINISHED, statuses.last())
    }
}
