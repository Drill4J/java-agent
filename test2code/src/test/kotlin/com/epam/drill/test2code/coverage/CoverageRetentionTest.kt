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

import com.epam.drill.common.agent.*
import com.epam.drill.jacoco.*
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.*
import java.math.*
import java.util.*
import kotlin.random.Random

class CoverageRetentionTest {

    // General services and vars at test execution
    private lateinit var sender: Sender
    private lateinit var coverageSender: CoverageSender
    private lateinit var inMemoryRetentionQueue: InMemoryRetentionQueue
    private lateinit var coverageTransport: CoverageTransport

    @BeforeEach
    fun setUp() {
        sender = mock()
        inMemoryRetentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(375))
        coverageTransport = spy(WebsocketCoverageTransport(UUID.randomUUID().toString(), sender))
        coverageSender = IntervalCoverageSender(
            intervalMs = 2500,
            coverageTransport = coverageTransport,
            inMemoryRetentionQueue = inMemoryRetentionQueue
        ) {
            sequenceOf(
                ExecDatum(
                    id = Random.nextLong(),
                    sessionId = "1",
                    probes = AgentProbes(initialSize = 3, values = booleanArrayOf(true, true, true)),
                    name = "foo/bar"
                )
            )
        }
    }

    @AfterEach
    fun tearDown() {
        coverageSender.stopSendingCoverage()
    }

    @Test
    fun `queue collect`() = runBlocking {
        coverageTransport.onUnavailable()
        coverageSender.startSendingCoverage()
        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        val flush = inMemoryRetentionQueue.flush().toList()
        //assert on none-empty buffer when coverageTransport.isAvailable() set to `false`
        assertTrue(flush.isNotEmpty())
        assertEquals(4, flush.size)
    }

    @Test
    fun `queue turn on sending onAvailable`() = runBlocking {
        coverageTransport.onUnavailable()
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        coverageTransport.onAvailable()
        delay(2600)
        verify(sender, times(5)).send(anyString(), anyString())
    }

    @Test
    fun `turn on queue data sending`() = runBlocking {
        coverageTransport.onAvailable()
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        verify(sender, times(4)).send(anyString(), anyString())

        coverageTransport.onUnavailable()
        delay(2500)
        val flush = inMemoryRetentionQueue.flush().toList()
        //assert on none-empty buffer when coverageTransport.isAvailable() set to `false`
        assertTrue(flush.isNotEmpty())
        assertEquals(1, flush.size)
    }

    @Test
    fun `queue limit exceeded`() = runBlocking {
        inMemoryRetentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(10))
        coverageSender = IntervalCoverageSender(
            intervalMs = 2500,
            coverageTransport = coverageTransport,
            inMemoryRetentionQueue = inMemoryRetentionQueue
        ) {
            sequenceOf(
                ExecDatum(
                    id = Random.nextLong(),
                    sessionId = "1",
                    probes = AgentProbes(initialSize = 3, values = booleanArrayOf(true, true, true)),
                    name = "foo/bar"
                )
            )
        }

        coverageTransport.onUnavailable()
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)

        val flush = inMemoryRetentionQueue.flush().toList()

        assertTrue(flush.isEmpty())
        assertEquals(0, flush.size)
    }

}
