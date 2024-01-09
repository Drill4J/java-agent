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
import com.epam.drill.test2code.JvmModuleConfiguration

open class CoverageManager(
    private val classDescriptorsManager: IClassDescriptorsManager = ConcurrentClassDescriptorsManager(),
    private val threadCoverageRecorder: ICoverageRecorder = ThreadCoverageRecorder(),
    private val globalCoverageRecorder: ICoverageRecorder = GlobalCoverageRecorder(),
    private val coverageSender: CoverageSender = IntervalCoverageSender(
        intervalMs = JvmModuleConfiguration.getSendCoverageInterval(),
        collectProbes = { globalCoverageRecorder.pollRecorded() + threadCoverageRecorder.pollRecorded() }
    )
) : IProbesProxy,
    IClassDescriptorStorage by classDescriptorsManager,
    ICoverageRecorder by threadCoverageRecorder,
    CoverageSender by coverageSender {

    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes {
        val coverage: ContextCoverage = threadCoverageRecorder.getCurrent()
            ?: globalCoverageRecorder.getCurrent()
            ?: return EmptyAgentProbes
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

//        val probesProvider = { context: ContextKey ->
//            val classDescriptor = classDescriptorsManager.get(id)
//            ExecDatum(
//                id = classDescriptor.id,
//                name = classDescriptor.name,
//                probes = AgentProbes(classDescriptor.probeCount),
//                sessionId = context.sessionId,
//                testId = context.testId
//            )
//        }
//        threadCoverageRecorder.write(id) { probesProvider(it) }
//        globalCoverageRecorder.write(id) { probesProvider(it) }
//        return execDatum.probes
    }
}

/**
 * The probes proxy MUST be a Kotlin singleton object
 */
internal object DrillCoverageManager : CoverageManager()

object EmptyAgentProbes : AgentProbes() {
    override fun set(index: Int) {
        // do nothing
    }
}