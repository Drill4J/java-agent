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

import com.epam.drill.plugins.test2code.common.api.*
import com.epam.drill.test2code.*
import io.aesy.datasize.*
import kotlinx.coroutines.*
import kotlinx.serialization.protobuf.*
import mu.*
import java.math.*
import java.text.*
import java.util.*
import java.util.concurrent.*

interface CoverageSender {
    fun setCoverageTransport(transport: CoverageTransport)
    fun startSendingCoverage()
    fun stopSendingCoverage()
}

class IntervalCoverageSender(
    private val logger: KLogger = KotlinLogging.logger("com.epam.drill.test2code.coverage.IntervalCoverageSender"),
    private val initialDelayMs: Long,
    private val intervalMs: Long,
    private var coverageTransport: CoverageTransport = StubTransport(),
    private val inMemoryRetentionQueue: RetentionQueue = InMemoryRetentionQueue(
        totalSizeByteLimit = try {
            DataSize.parse(JvmModuleConfiguration.getCoverageRetentionLimit())
                .toUnit(ByteUnit.BYTE)
                .value
                .toBigInteger()
        } catch (e: ParseException) {
            logger.warn { "Catch exception while parsing CoverageRetentionLimit. Exception: ${e.message}" }
            BigInteger.valueOf(1024 * 1024 * 512)
        }
    ),
    private val collectProbes: () -> Sequence<ExecDatum> = { emptySequence() }
) : CoverageSender {
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    override fun setCoverageTransport(transport: CoverageTransport) {
        coverageTransport = transport
    }

    override fun startSendingCoverage() {
        if (future == null) {
            future = scheduledThreadPool.scheduleAtFixedRate(
                Runnable { sendProbes(collectProbes()) },
                initialDelayMs,
                intervalMs,
                TimeUnit.MILLISECONDS
            )
        }
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
    private fun sendProbes(data: Sequence<ExecDatum>) {
        val dataToSend = data
            .map {
                ExecClassData(
                    id = it.id,
                    className = it.name,
                    probes = it.probes.values.toBitSet(),
                    sessionId = it.sessionId,
                    testId = it.testId,
                )
            }
            .chunked(0xffff)
            .map { chunk -> CoverDataPart(data = chunk) }
            .map { message ->
                ProtoBuf.encodeToByteArray(CoverMessage.serializer(), message)
            }

        if (coverageTransport.isAvailable()) {
            val failedToSend = mutableListOf<ByteArray>()

            val send = { message: ByteArray ->
                val encoded = Base64.getEncoder().encodeToString(message)
                try {
                    coverageTransport.send(encoded)
                } catch (e: Exception) {
                    failedToSend.add(message)
                }
            }

            dataToSend.forEach { send(it) }
            inMemoryRetentionQueue.flush().forEach { send(it) }
            if (failedToSend.size > 0) inMemoryRetentionQueue.addAll(failedToSend.asSequence())
        } else {
            inMemoryRetentionQueue.addAll(dataToSend)
        }
    }
}
