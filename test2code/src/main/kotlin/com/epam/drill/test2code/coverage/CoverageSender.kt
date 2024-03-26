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

import kotlinx.coroutines.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.agent.transport.ResponseStatus
import com.epam.drill.plugins.test2code.common.api.ExecClassData
import com.epam.drill.plugins.test2code.common.api.toBitSet
import com.epam.drill.plugins.test2code.common.transport.CoverageData

interface CoverageSender {
    fun setCoverageSendInterval(intervalMs: Long)
    fun setAgentMessageSender(sender: AgentMessageSender<in CoverageData>)
    fun startSendingCoverage()
    fun stopSendingCoverage()
}

class IntervalCoverageSender(
    private var intervalMs: Long,
    private var sender: AgentMessageSender<in CoverageData> = StubSender(),
    private val collectProbes: () -> Sequence<ExecDatum> = { emptySequence() }
) : CoverageSender {
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()
    private val destination = AgentMessageDestination("POST", "coverage")
    private val logger = KotlinLogging.logger {}

    override fun setCoverageSendInterval(intervalMs: Long) {
        this.intervalMs = intervalMs
    }

    override fun setAgentMessageSender(sender: AgentMessageSender<in CoverageData>) {
        this.sender = sender
    }

    override fun startSendingCoverage() {
        scheduledThreadPool.scheduleAtFixedRate(
            Runnable { sendProbes(collectProbes()) },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.debug { "Coverage sending job is started." }
    }

    override fun stopSendingCoverage() {
        scheduledThreadPool.shutdown()
        logger.debug { "Coverage sending job is stopped." }
    }

    /**
     * Create a function which sends chunks of test coverage to the admin part of the plugin
     * @return the function of sending test coverage
     * @features Coverage data sending
     */
    private fun sendProbes(dataToSend: Sequence<ExecDatum>) {
        dataToSend.map { ExecClassData(it.id, it.name, it.probes.values.toBitSet(), it.sessionId, it.testId) }
            .chunked(0xffff)
            .forEach { sender.send(destination, CoverageData(it)) }
    }

}

private class StubSender : AgentMessageSender<CoverageData> {
    override val available: Boolean = false
    override fun send(destination: AgentMessageDestination, message: CoverageData) = StubResponseStatus()
}

private class StubResponseStatus : ResponseStatus {
    override val success: Boolean = false
    override val statusObject: Any? = null
}
