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

interface IProbesProxy {
    fun invoke(id: ClassId, num: Int, name: String, probeCount: Int): AgentProbes
    fun addClassMethodsMetadata(classId: Long, methodsMetadata: ClassMethodsMetadata)
}

typealias ClassMethodsMetadata = Map<String, ClassMethodMetadata>
data class ClassMethodMetadata(
    val probesStartPos: Int,
    val probesCount: Int,
    val bodyChecksum: String
)

const val SESSION_CONTEXT_NONE = "SESSION_CONTEXT_NONE"
const val TEST_CONTEXT_NONE = "TEST_CONTEXT_NONE"
const val SESSION_CONTEXT_AMBIENT = "GLOBAL"

data class ContextKey(
    private val _sessionId: SessionId? = null, // TODO there must be a better way to avoid nullability and assign defaults
    private val _testId: TestId? = null
) {
    val sessionId: SessionId = _sessionId ?: SESSION_CONTEXT_NONE
    val testId: TestId = _testId ?: TEST_CONTEXT_NONE
}

internal val CONTEXT_AMBIENT = ContextKey(SESSION_CONTEXT_AMBIENT)