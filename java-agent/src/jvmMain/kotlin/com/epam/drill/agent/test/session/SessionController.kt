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
package com.epam.drill.agent.test.session

import com.benasher44.uuid.*
import com.epam.drill.agent.common.request.DrillInitialContext
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.request.DrillRequestHolder
import com.epam.drill.agent.test.SESSION_ID_HEADER
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.sending.IntervalTestInfoSender
import com.epam.drill.agent.test.sending.TestInfoSender
import com.epam.drill.agent.test.execution.TestController
import com.epam.drill.agent.test.execution.TestExecutionInfo
import com.epam.drill.agent.test.sending.TestDefinitionPayload
import com.epam.drill.agent.test.sending.TestLaunchPayload
import com.epam.drill.agent.test.transport.TestAgentMessageSender
import mu.KotlinLogging
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.CRC32

actual object SessionController {
    private val logger = KotlinLogging.logger {}
    private val sessionSender: SessionSender = SessionSenderImpl(
        messageSender = TestAgentMessageSender
    )
    private val testInfoSender: TestInfoSender = IntervalTestInfoSender(
        messageSender = TestAgentMessageSender,
        collectTests = { TestController.getFinishedTests().toTestLaunchPayloads() }
    )
    private lateinit var sessionId: String

    init {
        if (isTestLaunchMetadataSendingEnabled())
            Runtime.getRuntime().addShutdownHook(Thread { testInfoSender.stopSendingTests() })
    }

    actual fun startSession() {
        if (!isTestAgentEnabled()) {
            logger.info { "Test agent is disabled. Test session will not be started." }
            return
        }
        val customSessionId = Configuration.parameters[ParameterDefinitions.SESSION_ID]
        sessionId = customSessionId ?: uuid4().toString()
        DrillInitialContext.add(SESSION_ID_HEADER, sessionId)
        DrillRequestHolder.store(DrillRequest(sessionId))
        logger.info { "Test session started: $sessionId" }
        val builds =
            takeIf { Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_APP_ID].isNotEmpty() }?.let {
                SingleSessionBuildPayload(
                    appId = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_APP_ID],
                    buildVersion = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_BUILD_VERSION],
                    commitSha = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_COMMIT_SHA]
                )
            }?.let { listOf(it) } ?: emptyList()
        sessionSender.sendSession(
            SessionPayload(
                id = sessionId,
                groupId = Configuration.parameters[DefaultParameterDefinitions.GROUP_ID],
                testTaskId = Configuration.parameters[ParameterDefinitions.TEST_TASK_ID],
                startedAt = System.currentTimeMillis().toIsoTimeFormat(),
                builds = builds
            )
        )
        if (isTestLaunchMetadataSendingEnabled())
            testInfoSender.startSendingTests()
    }

    fun getSessionId(): String = sessionId

    private fun isTestAgentEnabled(): Boolean = Configuration.parameters[ParameterDefinitions.TEST_AGENT_ENABLED]
    private fun isTestLaunchMetadataSendingEnabled(): Boolean = isTestAgentEnabled() && Configuration.parameters[ParameterDefinitions.TEST_LAUNCH_METADATA_SENDING_ENABLED]
}

private fun List<TestExecutionInfo>.toTestLaunchPayloads(): List<TestLaunchPayload> = map { info ->
    val testDefinitionPayload = TestDefinitionPayload(
        runner = info.testMethod.engine,
        path = info.testMethod.className,
        testName = info.testMethod.method,
        testParams = info.testMethod.methodParams.removeSurrounding("(", ")").split(",").filter { it.isNotEmpty() },
        metadata = info.testMethod.metadata,
        tags = info.testMethod.tags
    )
    TestLaunchPayload(
        testLaunchId = info.testLaunchId,
        testDefinitionId = hash(info.testMethod.signature),
        result = info.result,
        duration = info.finishedAt?.minus(info.startedAt ?: 0)?.toInt(),
        details = testDefinitionPayload
    )
}

private fun hash(signature: String): String = CRC32().let {
    it.update(signature.toByteArray())
    java.lang.Long.toHexString(it.value)
}

private fun Long.toIsoTimeFormat(): String = Instant.ofEpochMilli(this)
    .let { ZonedDateTime.ofInstant(it, ZoneId.systemDefault()) }
    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
