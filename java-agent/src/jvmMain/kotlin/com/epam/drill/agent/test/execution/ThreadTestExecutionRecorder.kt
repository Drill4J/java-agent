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
package com.epam.drill.agent.test.execution

import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import com.epam.drill.agent.test.TEST_ID_HEADER
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.session.SessionController
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ThreadTestExecutionRecorder(
    private val requestHolder: RequestHolder,
    private val listeners: List<TestExecutionListener> = emptyList()
) : TestExecutionRecorder {
    private val logger = KotlinLogging.logger {}
    private val testExecutionData: ConcurrentHashMap<String, TestExecutionInfo> = ConcurrentHashMap()
    private val testLaunchHolder: ThreadLocal<String> = ThreadLocal.withInitial { null }

    override fun recordTestStarting(
        testMethod: TestMethodInfo
    ) {
        val testLaunchId = generateTestLaunchId()
        testLaunchHolder.set(testLaunchId)
        updateTestInfo(testLaunchId, testMethod) {
            it.startedAt = System.currentTimeMillis()
        }
        addDrillHeaders(testLaunchId)
        listeners.forEach { it.onTestStarted(testLaunchId, testMethod) }
        logger.debug { "Test: $testMethod STARTED" }
    }

    override fun recordTestFinishing(
        testMethod: TestMethodInfo,
        status: String
    ) {
        val testLaunchId = testLaunchHolder.get()
        if (testLaunchId == null) {
            logger.warn { "Test ${testMethod.className}::${testMethod.method} finished with result $status but no test launch id was found." }
            return
        }
        testLaunchHolder.remove()
        val testResult = mapToTestResult(status)
        updateTestInfo(testLaunchId, testMethod) {
            it.finishedAt = System.currentTimeMillis()
            it.result = testResult
        }
        clearDrillHeaders()
        listeners.forEach { it.onTestFinished(testLaunchId, testMethod, testResult) }
        println("Test: $testMethod FINISHED")
        logger.debug { "Test: $testMethod FINISHED. Result: $status" }
    }

    override fun recordTestIgnoring(
        testMethod: TestMethodInfo,
        isSmartSkip: Boolean
    ) {
        val testLaunchId = testLaunchHolder.get() ?: generateTestLaunchId()
        testLaunchHolder.remove()
        val skipResult = if (isSmartSkip) TestResult.SMART_SKIPPED else TestResult.SKIPPED
        updateTestInfo(testLaunchId, testMethod) {
            it.startedAt = null
            it.finishedAt = null
            it.result = skipResult
        }
        clearDrillHeaders()
        listeners.forEach { it.onTestIgnored(testLaunchId, testMethod) }
        logger.debug { "Test: $testMethod FINISHED. Result: $skipResult" }
    }

    override fun reset() {
        testExecutionData.clear()
    }

    override fun getFinishedTests(): List<TestExecutionInfo> = testExecutionData
        .filterValues { test -> test.result != TestResult.UNKNOWN }
        .onEach {
            testExecutionData.remove(it.key)
        }.values.toList()

    private fun updateTestInfo(
        testLaunchId: String,
        testMethodInfo: TestMethodInfo,
        updateTestExecutionInfo: (TestExecutionInfo) -> Unit,
    ) {
        testExecutionData.compute(testLaunchId) { _, value ->
            val testExecutionInfo = value ?: TestExecutionInfo(testLaunchId, testMethodInfo)
            if (testExecutionInfo.result == TestResult.UNKNOWN) {
                updateTestExecutionInfo(testExecutionInfo)
            } else {
                logger.warn { "Test ${testMethodInfo.method} already finished with result ${testExecutionInfo.result}" }
            }
            testExecutionInfo
        }
    }

    private fun generateTestLaunchId() = UUID.randomUUID().toString()

    private fun addDrillHeaders(testLaunchId: String) {
        val drillRequest = if (Configuration.parameters[ParameterDefinitions.TEST_TRACING_ENABLED]) {
            DrillRequest(
                drillSessionId = SessionController.getSessionId(),
                headers = mapOf(TEST_ID_HEADER to (testLaunchId))
            )
        } else {
            DrillRequest(drillSessionId = SessionController.getSessionId())
        }
        requestHolder.store(drillRequest)
    }

    private fun clearDrillHeaders() {
        requestHolder.remove()
    }

    private fun mapToTestResult(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }
}