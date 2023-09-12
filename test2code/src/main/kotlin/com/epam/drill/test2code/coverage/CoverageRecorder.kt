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

import mu.KotlinLogging

interface ICoverageRecorder {
    fun startRecording(sessionId: String, testId: String)
    fun stopRecording(sessionId: String, testId: String)
    fun collectProbes(): Sequence<ExecDatum>
}

class CoverageRecorder(
    private val execDataPool: DataPool<SessionTestKey, ExecData>,
    private val execDataProvider: IExecDataProvider,
) : ICoverageRecorder {
    private val logger = KotlinLogging.logger {}

    override fun startRecording(sessionId: String, testId: String) {
        execDataProvider.setContext(sessionId, testId)
        logger.trace { "Test recording started (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun stopRecording(sessionId: String, testId: String) {
        val data = execDataProvider.getExecData(sessionId, testId)
        if (data != null) {
            execDataPool.release(SessionTestKey(sessionId, testId), data)
        }
        logger.trace { "Test recording stopped (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun collectProbes(): Sequence<ExecDatum> {
        return execDataPool.pollReleased().flatMap { it.values }.filter { it.probes.containCovered() }
    }
}