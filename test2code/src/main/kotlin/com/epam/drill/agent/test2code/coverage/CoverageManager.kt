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

open class CoverageManager(
    private val classDescriptorsManager: IClassDescriptorsManager = ConcurrentClassDescriptorsManager(),
    private val threadCoverageRecorder: ICoverageRecorder = ThreadCoverageRecorder(),
    private val globalCoverageRecorder: GlobalCoverageRecorder = GlobalCoverageRecorder(),
) : IProbesProxy,
    IClassDescriptorStorage by classDescriptorsManager,
    ICoverageRecorder by threadCoverageRecorder {

    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes {
        val coverage: ContextCoverage = threadCoverageRecorder.getContext()
            ?: globalCoverageRecorder.getContext()
        val execDatum = coverage.execData.getOrPut(id) {
            val classDescriptor = classDescriptorsManager.get(id)
            ExecDatum(
                id = classDescriptor.id,
                name = classDescriptor.name,
                probes = AgentProbes(classDescriptor.probeCount),
                sessionId = coverage.context.sessionId,
                testId = coverage.context.testId
            )
        }
        return execDatum.probes
    }

    override fun pollRecorded(): Sequence<ExecDatum> {
        return threadCoverageRecorder.pollRecorded() + globalCoverageRecorder.pollRecorded()
    }

}

/**
 * The probes proxy MUST be a Kotlin singleton object
 */
internal object DrillCoverageManager : CoverageManager()