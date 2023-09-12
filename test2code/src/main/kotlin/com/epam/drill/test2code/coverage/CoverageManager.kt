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

/**
 * Simple probe provider that employs a lock-free map for runtime data storage.
 * This class is intended to be an ancestor for a concrete probe array provider object.
 * The provider must be a Kotlin singleton object, otherwise the instrumented probe calls will fail.
 */
open class CoverageManager(
    private val execDataPool: DataPool<SessionTestKey, ExecData> = ConcurrentDataPool(),
    private val probesDescriptorProvider: IClassDescriptorProvider = ConcurrentClassDescriptorProvider(),
    private val execDataProvider: IExecDataProvider = ThreadExecDataProvider(execDataPool, probesDescriptorProvider),
    private val coverageRecorder: ICoverageRecorder = CoverageRecorder(
        execDataPool,
        execDataProvider
    ),
    private val coverageSender: CoverageSender = IntervalCoverageSender(2000L) {
        coverageRecorder.collectProbes().map { datum ->
            datum.copy(
                probes = AgentProbes(
                    values = datum.probes.values.copyOf()
                )
            ).also {
                datum.probes.values.fill(false)
            }
        }
    }
) : IProbesProxy by execDataProvider,
    IClassDescriptorProvider by probesDescriptorProvider,
    ICoverageRecorder by coverageRecorder,
    CoverageSender by coverageSender

/**
 * The probe provider MUST be a Kotlin singleton object
 */
internal object DrillCoverageManager : CoverageManager()