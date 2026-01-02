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
package com.epam.drill.agent.test2code.coverage

import com.epam.drill.agent.jacoco.AgentProbes

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias IProbesProxy = (ClassId, Int, String, Int) -> AgentProbes

const val SESSION_CONTEXT_NONE = "SESSION_CONTEXT_NONE"
const val TEST_CONTEXT_NONE = "TEST_CONTEXT_NONE"
const val SESSION_CONTEXT_AMBIENT = "GLOBAL"

data class ContextKey(
    var sessionId: SessionId,
    var testId: TestId
) {
    fun clear() {
        sessionId = SESSION_CONTEXT_NONE
        testId = TEST_CONTEXT_NONE
    }
    fun isSessionEmpty() = sessionId == SESSION_CONTEXT_NONE
    fun isSessionGlobal() = sessionId == SESSION_CONTEXT_AMBIENT
}

internal val CONTEXT_AMBIENT = ContextKey(SESSION_CONTEXT_AMBIENT, TEST_CONTEXT_NONE)