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
import org.junit.Ignore
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.*
import java.math.*
import java.util.*
import kotlin.random.Random

@Ignore
class CoverageRetentionTest {

    private lateinit var sender: Sender
    private lateinit var coverageSender: CoverageSender
    private lateinit var inMemoryRetentionQueue: InMemoryRetentionQueue
    private lateinit var coverageTransport: CoverageTransport

    @BeforeEach
    fun setUp() {
        sender = mock()
        inMemoryRetentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = BigInteger.valueOf(375))
        coverageTransport = spy(WebsocketCoverageTransport(UUID.randomUUID().toString(), sender))
        coverageSender = IntervalCoverageSender(2500, inMemoryRetentionQueue) {
            sequenceOf(
                ExecDatum(
                    id = Random.nextLong(),
                    sessionId = "1",
                    probes = AgentProbes(initialSize = 3, values = booleanArrayOf(true, true, true)),
                    name = "foo/bar"
                )
            )
        }.apply { setCoverageTransport(coverageTransport) }
    }

    @AfterEach
    fun tearDown() {
        coverageSender.stopSendingCoverage()
    }

    @Test
    fun `send and shutdown connection with admin should fill the queue`() = runBlocking {
        whenever(coverageTransport.isAvailable()).thenReturn(true)
        coverageSender.startSendingCoverage()
        delay(2000)

        whenever(coverageTransport.isAvailable()).thenReturn(false)

        delay(2000)
        val flush = inMemoryRetentionQueue.flush().toList()

        assertTrue(flush.isNotEmpty())
        assertEquals(1, flush.size)
    }

    @Test
    fun `shutdown connection with admin and send action should fill the queue`() = runBlocking {
        whenever(coverageTransport.isAvailable()).thenReturn(false)
        coverageSender.startSendingCoverage()

        delay(2000)
        val flush = inMemoryRetentionQueue.flush().toList()

        assertTrue(flush.isNotEmpty())
        assertEquals(1, flush.size)
    }

}
