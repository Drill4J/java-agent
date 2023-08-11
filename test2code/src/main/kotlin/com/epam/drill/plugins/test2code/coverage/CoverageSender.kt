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
package com.epam.drill.plugins.test2code.coverage

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

interface CoverageSender {
    fun setSendingHandler(handler: SendingHandler)
    fun startSendingCoverage()
    fun stopSendingCoverage()
}

class IntervalCoverageSender(
    intervalMs: Long,
    collectProbes: () -> Sequence<ExecDatum> = { emptySequence() }
) : CoverageSender {
    private val logger = KotlinLogging.logger {}
    private var sendingHandler: SendingHandler = {}

    private val job = ProbeWorker.launch(start = CoroutineStart.LAZY) {
        while (isActive) {
            delay(intervalMs)
            sendingHandler(collectProbes())
        }
    }

    override fun setSendingHandler(handler: SendingHandler) {
        sendingHandler = handler
    }

    override fun startSendingCoverage() {
        job.start()
        logger.debug { "Coverage sending job is started." }
    }

    override fun stopSendingCoverage() {
        job.cancel()
        logger.debug { "Coverage sending job is stopped." }
    }
}

internal object ProbeWorker : CoroutineScope {
    override val coroutineContext: CoroutineContext = run {
        // TODO ProbeWorker thread count configure via env.variable?
        Executors.newFixedThreadPool(2).asCoroutineDispatcher() + SupervisorJob()
    }
}