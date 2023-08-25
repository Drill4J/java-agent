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
package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import com.epam.drill.plugins.test2code.common.api.*
import java.util.concurrent.ConcurrentHashMap

typealias ClassId = Long
typealias SessionId = String
typealias TestId = String
typealias SessionTestKey = Pair<SessionId, TestId>
typealias ExecData = ConcurrentHashMap<ClassId, ExecDatum>



/**
 * A class containing probes obtained from a specific test
 */
data class ExecDatum(
    val id: ClassId,
    val name: String,
    val probes: AgentProbes,
    val sessionId: String,
    val testId: String = DEFAULT_TEST_ID,
)