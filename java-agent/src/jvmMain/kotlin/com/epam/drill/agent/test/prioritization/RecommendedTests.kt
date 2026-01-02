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

import com.epam.drill.agent.test.execution.TestMethodInfo
import mu.KotlinLogging

object RecommendedTests {
    private val logger = KotlinLogging.logger {}
    private val recommendedTestsReceiver: RecommendedTestsReceiver = RecommendedTestsReceiverImpl()
    private val testsToSkip: Set<TestMethodInfo> by lazy { initTestsToSkip() }

    private fun initTestsToSkip() = recommendedTestsReceiver.getTestsToSkip()
        .toSet()
        .also {
            logger.info { "${it.size} tests will be skipped by Drill4J" }
        }


    fun shouldSkip(
        engine: String,
        testClass: String,
        testMethod: String,
        methodParameters: String = "()"
    ): Boolean {
        val test = TestMethodInfo(
            engine = engine,
            className = testClass,
            method = testMethod,
            methodParams = methodParameters,
        )
        return shouldSkipByTestMethod(test)
    }

    fun shouldSkipByTestMethod(test: TestMethodInfo): Boolean {
        return testsToSkip.contains(test).also {
            if (it) {
                logger.debug { "Test `${test.method}` will be skipped by Drill4J" }
                recommendedTestsReceiver.sendSkippedTest(test)
            } else {
                logger.debug { "Test `${test.method}` will not be skipped by Drill4J" }
            }
        }
    }

}