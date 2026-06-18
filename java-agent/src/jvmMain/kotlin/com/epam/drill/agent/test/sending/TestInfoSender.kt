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
package com.epam.drill.agent.test.sending

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.session.SessionController
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface TestInfoSender {
    fun startSendingTests()
    fun stopSendingTests(remainingMs: Long)
}

class IntervalTestInfoSender(
    private val messageSender: AgentMessageSender,
    private val intervalMs: Long = 1000,
    private val collectTests: () -> List<TestLaunchPayload> = { emptyList() }
) : TestInfoSender {
    private val logger = KotlinLogging.logger {}
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "drill-test-info-sender").apply {
            isDaemon = true
        }
    }

    override fun startSendingTests() {
        scheduledThreadPool.scheduleAtFixedRate(
            {
                try {
                    sendTests(collectTests())
                } catch (t: Throwable) {
                    logger.error(t) { "Test sending job failed" }
                }
            },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.debug { "Test sending job is started." }
    }

    override fun stopSendingTests(remainingMs: Long) {
        sendTests(collectTests())
        scheduledThreadPool.shutdown()
        if (remainingMs > 0 && !scheduledThreadPool.awaitTermination(remainingMs, TimeUnit.MILLISECONDS)) {
            logger.warn { "Test sending scheduler did not stop within ${remainingMs}ms; leaving it for JVM exit." }
        }
        logger.info { "Test sending job is stopped." }
    }

    private fun sendTests(tests: List<TestLaunchPayload>) {
        if (tests.isEmpty()) return
        logger.debug { "Sending ${tests.size} tests..." }
        messageSender.send(
            destination = AgentMessageDestination("POST", "tests-metadata"),
            message = AddTestsPayload(
                groupId = Configuration.parameters[DefaultParameterDefinitions.GROUP_ID],
                sessionId = SessionController.getSessionId(),
                tests = tests
            ),
            serializer = AddTestsPayload.serializer()
        )
    }
}