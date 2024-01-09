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

class ThreadCoverageRecorder(
    private val execDataPool: DataPool<ContextKey, ExecData> = ConcurrentDataPool()
) : ICoverageRecorder {
    private val logger = KotlinLogging.logger {}
    private val context: ThreadLocal<ContextKey?> = ThreadLocal()
    private val execData: ThreadLocal<ExecData?> = ThreadLocal()

    override fun startRecording(sessionId: String, testId: String) {
        val ctx = ContextKey(sessionId, testId)
        context.set(ctx)
        execData.set(execDataPool.getOrPut(
            ctx,
            default = { ExecData() }
        ))
        logger.trace { "Test recording started (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun stopRecording(sessionId: String, testId: String) {
        execDataPool.release(ContextKey(sessionId, testId), execData.get() ?: ExecData())
        execData.remove()
        context.remove()
        logger.trace { "Test recording stopped (sessionId = $sessionId, testId=$testId, threadId=${Thread.currentThread().id})." }
    }

    override fun pollRecorded(): Sequence<ExecDatum> {
        return execDataPool
            .pollReleased()
            .flatMap { it.values }
            .filter { it.probes.containCovered() }
    }

    override fun getContext(): ContextCoverage? {
        return context.get()?.let { ctx -> execData.get()?.let { ContextCoverage(ctx, it) } }
    }
}

