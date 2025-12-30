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
    fun stopSendingTests()
}

class IntervalTestInfoSender(
    private val messageSender: AgentMessageSender,
    private val intervalMs: Long = 1000,
    private val collectTests: () -> List<TestLaunchPayload> = { emptyList() }
) : TestInfoSender {
    private val logger = KotlinLogging.logger {}
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()

    override fun startSendingTests() {
        scheduledThreadPool.scheduleAtFixedRate(
            { sendTests(collectTests()) },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.debug { "Test sending job is started." }
    }

    override fun stopSendingTests() {
        scheduledThreadPool.shutdown()
        if (!scheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            logger.error("Failed to send some tests prior to shutdown")
            scheduledThreadPool.shutdownNow();
        }
        sendTests(collectTests())
        messageSender.shutdown()
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