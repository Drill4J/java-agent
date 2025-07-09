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

class GlobalCoverageRecorder: ICoverageRecorder {
    private val globalExecData: ExecData = ExecData()
    private val sentGlobalExecData: ExecData = ExecData()

    override fun startRecording(sessionId: String?, testId: String?) {
        // do nothing
    }

    override fun stopRecording(sessionId: String?, testId: String?) {
        // do nothing
    }

    override fun getContext(): ContextCoverage {
        return ContextCoverage(CONTEXT_AMBIENT, globalExecData)
    }

    override fun pollRecorded(): Sequence<ExecDatum> {
        val unsentExecData = ExecData()
        globalExecData.forEach { (key, value) ->
            sentGlobalExecData.compute(key) { _, oldValue ->
                if (oldValue?.equals(value) != true) {
                    val copiedValue = value.copy(probes = AgentProbes(values = value.probes.values.copyOf()))
                    unsentExecData[key] = copiedValue
                    copiedValue
                } else
                    oldValue
            }
        }
        return if (unsentExecData.isNotEmpty()) {
            unsentExecData.values.asSequence().filter { it.probes.containCovered() }
        } else
            emptySequence()
    }
}
