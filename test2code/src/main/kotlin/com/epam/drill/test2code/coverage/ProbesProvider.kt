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
import com.epam.drill.jacoco.StubAgentProbes

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias ProbesProvider = (ClassId, Int, String, Int) -> AgentProbes


class SimpleProbesProvider(
    private val requestThreadLocal: ThreadLocal<ExecData?>,
    private var globalExecData: ExecData
) : ProbesProvider {
    private val stubProbes = StubAgentProbes()
    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = getSessionClassProbes(id)
        ?: getGlobalClassProbes(id)
        ?: stubProbes

    /**
     * requestThreadLocal stores probes of classes for a specific session
     * (see requestThreadLocal.set(execData) in processServerRequest method in Plugin.kt)
     */
    private fun getSessionClassProbes(id: ClassId) = requestThreadLocal.get()?.get(id)?.probes

    private fun getGlobalClassProbes(id: ClassId) = globalExecData[id]?.probes
}