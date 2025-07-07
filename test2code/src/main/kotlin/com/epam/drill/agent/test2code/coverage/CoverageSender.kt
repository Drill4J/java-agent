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

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageSender
import com.epam.drill.agent.test2code.common.api.ClassCoverage
import com.epam.drill.agent.test2code.common.api.toBitSet
import com.epam.drill.agent.test2code.common.transport.CoveragePayload

interface CoverageSender {
    fun startSendingCoverage()
    fun stopSendingCoverage()
}

class IntervalCoverageSender(
    private val groupId: String,
    private val appId: String,
    private val instanceId: String,
    private val intervalMs: Long,
    private val pageSize: Int,
    private val sender: AgentMessageSender<in CoveragePayload> = StubSender(),
    private val collectProbes: () -> Sequence<ExecDatum> = { emptySequence() }
) : CoverageSender {
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()
    private val destination = AgentMessageDestination("POST", "coverage")
    private val logger = KotlinLogging.logger {}

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
        if (!scheduledThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.error("Failed to send some coverage data prior to shutdown")
            scheduledThreadPool.shutdownNow();
        }
        sendProbes(collectProbes())
        sender.shutdown()
        logger.info { "Coverage sending job is stopped." }
    }

    /**
     * Create a function which sends chunks of test coverage to the admin part of the plugin
     * @return the function of sending test coverage
     * @features Coverage data sending
     */
    private fun sendProbes(dataToSend: Sequence<ExecDatum>) {
        dataToSend
            .map { ClassCoverage(
                classname = it.name,
                testSessionId = it.sessionId,
                testId = it.testId,
                probes = it.probes.values.toBitSet()) }
            .chunked(pageSize)
            .forEach { sender.send(destination, CoveragePayload(groupId, appId, instanceId, it)) }
    }

}

private class StubSender : AgentMessageSender<CoveragePayload> {
    override fun send(destination: AgentMessageDestination, message: CoveragePayload) {}
}
