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
package com.epam.drill.plugins.test2code

import com.epam.drill.jacoco.AgentProbes
import com.epam.drill.plugins.test2code.common.api.ExecClassData
import com.epam.drill.plugins.test2code.common.api.toBitSet
import java.util.concurrent.ConcurrentHashMap

// key - classId ; value - ExecDatum
typealias ExecData = ConcurrentHashMap<Long, ExecDatum>
data class ExecDatum(
    val id: Long,
    val name: String,
    val probes: AgentProbes,
    val sessionId: String = "",
    val testName: String = "",
    val testId: String = "",
)

internal fun ExecDatum.toExecClassData() = ExecClassData(
    id = id,
    className = name,
    probes = probes.values.toBitSet(),
    testName = testName,
    testId = testId,
)

fun ExecData.hasProbes(): Boolean {
    return values.any { data -> data.probes.values.any { it } }
}