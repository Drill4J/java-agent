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

import com.epam.drill.agent.request.DrillRequestHolder
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.TEST_ID_HEADER
import com.epam.drill.agent.test.devtools.ChromeDevToolTestExecutionListener
import com.epam.drill.agent.test.devtools.JsCoverageSenderImpl

object TestController : TestExecutionRecorder by testExecutionRecorder(testExecutionListeners()) {

    //TODO EPMDJ-10251 add browser name for ui tests
    @JvmOverloads
    fun testStarted(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        testTags: List<String> = emptyList(),
    ) {
        if (className == null || method == null)
            return

        recordTestStarting(
            TestMethodInfo(
                engine = engine,
                className = className,
                method = method,
                methodParams = methodParams,
                tags = testTags
            )
        )
    }

    @JvmOverloads
    fun testFinished(
        engine: String,
        className: String?,
        method: String?,
        status: String,
        methodParams: String = "()",
        testTags: List<String> = emptyList(),
    ) {
        if (className == null || method == null)
            return

        recordTestFinishing(
            TestMethodInfo(
                engine = engine,
                className = className,
                method = method,
                methodParams = methodParams,
                tags = testTags
            ),
            status
        )
    }

    @JvmOverloads
    fun testIgnored(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        testTags: List<String> = emptyList(),
    ) {
        if (className == null || method == null)
            return

        recordTestIgnoring(
            TestMethodInfo(
                engine = engine,
                className = className,
                method = method,
                methodParams = methodParams,
                tags = testTags
            )
        )
    }

    @Deprecated("Use explicit retrieve() instead", ReplaceWith("retrieve()"))
    fun getTestLaunchId(): String? = DrillRequestHolder.retrieve()?.headers?.get(TEST_ID_HEADER)
}

fun testExecutionListeners(): List<TestExecutionListener> {
    val listeners = arrayListOf<TestExecutionListener>()
    if (Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE]) {
        listeners.add(
            ChromeDevToolTestExecutionListener(
                jsCoverageSender = JsCoverageSenderImpl()
            )
        )
    }
    return listeners
}

fun testExecutionRecorder(listeners: List<TestExecutionListener>) = ThreadTestExecutionRecorder(
    requestHolder = DrillRequestHolder,
    listeners = listeners
)