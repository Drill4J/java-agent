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
package com.epam.drill.agent.test.prioritization

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageReceiver
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.execution.TestController
import com.epam.drill.agent.test.execution.TestExecutionRecorder
import com.epam.drill.agent.test.execution.TestMethodInfo
import com.epam.drill.agent.transport.MetricsMessageReceiver
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File

interface RecommendedTestsReceiver {
    fun getTestsToSkip(): List<TestMethodInfo>
    fun sendSkippedTest(test: TestMethodInfo)
}

class RecommendedTestsReceiverImpl(
    private val agentMessageReceiver: AgentMessageReceiver = MetricsMessageReceiver,
    private val testExecutionRecorder: TestExecutionRecorder = TestController
) : RecommendedTestsReceiver {
    private val logger = KotlinLogging.logger {}

    override fun getTestsToSkip(): List<TestMethodInfo> {
        val testsToSkipFilePath = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_FILE]
        if (testsToSkipFilePath != null) {
            return loadTestsToSkipFromFile(testsToSkipFilePath)
        }
        if (!Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED])
            return emptyList()
        val groupId = Configuration.parameters[DefaultParameterDefinitions.GROUP_ID]
        val targetAppId = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_APP_ID]
        val targetBuildVersion = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_BUILD_VERSION]
            .takeIf { it.isNotEmpty() }
        val targetCommitSha = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_COMMIT_SHA]
            .takeIf { it.isNotEmpty() }
        val baselineCommitSha = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_BASELINE_COMMIT_SHA]
            .takeIf { it.isNotEmpty() }
        val baselineBuildVersion =
            Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_BASELINE_BUILD_VERSION]
                .takeIf { it.isNotEmpty() }
        val limit = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_LIMIT]
        val parameters: String = buildString {
            append("?groupId=$groupId")
            append("&appId=$targetAppId")
            targetBuildVersion?.let { append("&buildVersion=$it") }
            targetCommitSha?.let { append("&commitSha=$it") }
            baselineCommitSha?.let { append("&baselineCommitSha=$it") }
            baselineBuildVersion?.let { append("&baselineBuildVersion=$it") }
            append("&impactStatuses=NOT_IMPACTED")
            append("&pageSize=$limit")
        }
        logger.debug { "Retrieving information about recommended tests..." }
        return runCatching {
            val response = agentMessageReceiver.receive(
                AgentMessageDestination(
                    "GET",
                    "/impacted-tests$parameters",
                ),
                RecommendedTestsApiResponse::class
            )
            if (response.paging.pageSize >= limit) {
                logger.warn { "The number of recommended tests is more or equal than $limit. Consider increasing the limit." }
            }
            response.data.map { it.toTestMethodInfo() }
        }.onFailure {
            logger.warn { "Unable to retrieve information about recommended tests. Error message: $it" }
        }.getOrElse {
            emptyList()
        }
    }

    override fun sendSkippedTest(test: TestMethodInfo) {
        testExecutionRecorder.recordTestIgnoring(test, isSmartSkip = true)
    }

    private fun loadTestsToSkipFromFile(filePath: String): List<TestMethodInfo> {
        logger.debug { "Loading tests to skip from file: $filePath" }
        return runCatching {
            val content = File(filePath).readText()
            val entries = fileJson.decodeFromString(ListSerializer(TestDefinitionResponse.serializer()), content)
            entries.map { it.toTestMethodInfo() }.also {
                logger.info { "Loaded ${it.size} tests to skip from file: $filePath" }
            }
        }.onFailure {
            logger.warn { "Unable to load tests to skip from file '$filePath'. Error message: $it" }
        }.getOrElse {
            emptyList()
        }
    }
}

private val fileJson = Json { ignoreUnknownKeys = true }

@Serializable
class RecommendedTestsApiResponse(
    val data: List<TestDefinitionResponse>,
    val paging: Paging
)

@Serializable
class TestDefinitionResponse(
    val testDefinitionId: String,
    val testRunner: String,
    val testPath: String,
    val testName: String,
    val tags: List<String>,
    val metadata: Map<String, String>,
    val impactStatus: String,
)

@Serializable
data class Paging(
    val page: Int,
    val pageSize: Int,
    val total: Long?
)

private fun TestDefinitionResponse.toTestMethodInfo() = TestMethodInfo(
    engine = testRunner,
    className = testPath,
    method = testName,
    metadata = metadata,
    tags = tags,
)