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
import com.epam.drill.agent.test.session.SessionController.getSessionId
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
    private val collectTestDefinitions: () -> List<TestDefinitionPayload>,
    private val collectTestLaunches: () -> List<TestLaunchPayload>,
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
                    sendTestDefinitions(collectTestDefinitions())
                } catch (t: Throwable) {
                    logger.error(t) { "Test definition sending job failed" }
                }
                try {
                    sendTestLaunches(collectTestLaunches())
                } catch (t: Throwable) {
                    logger.error(t) { "Test launch sending job failed" }
                }
            },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.debug { "Test sending job is started." }
    }

    override fun stopSendingTests(remainingMs: Long) {
        sendTestDefinitions(collectTestDefinitions())
        sendTestLaunches(collectTestLaunches())
        scheduledThreadPool.shutdown()
        if (remainingMs > 0 && !scheduledThreadPool.awaitTermination(remainingMs, TimeUnit.MILLISECONDS)) {
            logger.warn { "Test sending scheduler did not stop within ${remainingMs}ms; leaving it for JVM exit." }
        }
        logger.info { "Test sending job is stopped." }
    }

    private fun sendTestLaunches(launches: List<TestLaunchPayload>) {
        if (launches.isEmpty()) return
        logger.debug { "Sending ${launches.size} test launches..." }
        messageSender.send(
            destination = AgentMessageDestination("POST", "test-launches"),
            message = AddTestLaunchesPayload(
                groupId = Configuration.parameters[DefaultParameterDefinitions.GROUP_ID],
                testProjectId = Configuration.parameters[ParameterDefinitions.TEST_PROJECT_ID],
                testSessionId = getSessionId(),
                launches = launches
            ),
            serializer = AddTestLaunchesPayload.serializer()
        )
    }

    private fun sendTestDefinitions(definitions: List<TestDefinitionPayload>) {
        if (definitions.isEmpty()) return
        logger.debug { "Sending ${definitions.size} test definitions..." }
        messageSender.send(
            destination = AgentMessageDestination("POST", "test-definitions"),
            message = AddTestDefinitionsPayload(
                groupId = Configuration.parameters[DefaultParameterDefinitions.GROUP_ID],
                testProjectId = Configuration.parameters[ParameterDefinitions.TEST_PROJECT_ID],
                definitions = definitions
            ),
            serializer = AddTestDefinitionsPayload.serializer()
        )
    }
}