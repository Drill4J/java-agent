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
package com.epam.drill.agent.test.devtools

import com.epam.drill.agent.test.instrument.selenium.DevToolStorage
import com.epam.drill.agent.test.instrument.selenium.WebDriverThreadStorage
import com.epam.drill.agent.test.execution.TestExecutionListener
import com.epam.drill.agent.test.execution.TestMethodInfo
import com.epam.drill.agent.test.execution.TestResult

class ChromeDevToolTestExecutionListener(
    private val jsCoverageSender: JsCoverageSender
): TestExecutionListener {

    override fun onTestStarted(testLaunchId: String, test: TestMethodInfo) {
        DevToolStorage.get()?.startIntercept()
        WebDriverThreadStorage.addCookies()
    }

    override fun onTestFinished(testLaunchId: String, test: TestMethodInfo, result: TestResult) {
        DevToolStorage.get()?.stopIntercept()
        jsCoverageSender.sendJsCoverage(testLaunchId)
    }

    override fun onTestIgnored(testLaunchId: String, test: TestMethodInfo) {
        DevToolStorage.get()?.stopIntercept()
    }
}