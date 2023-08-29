package com.epam.drill.test2code.coverage

import com.epam.drill.common.agent.*
import com.epam.drill.jacoco.*
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.random.Random

class CoverageRetentionTest {

    // General services and vars at test execution
    private lateinit var sender: Sender
    private lateinit var coverageSender: CoverageSender
    private lateinit var inMemoryBuffer: InMemoryBuffer
    private lateinit var coverageTransport: CoverageTransport

    @BeforeEach
    fun setUp() {
        sender = mock()
        inMemoryBuffer = InMemoryBuffer()
        coverageTransport = spy(WebsocketCoverageTransport(UUID.randomUUID().toString(), sender))
        coverageSender = IntervalCoverageSender(2500, coverageTransport, inMemoryBuffer) {
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

    @Test
    fun `buffer collect`() = runBlocking {
        coverageTransport.onUnavailable()
        coverageSender.startSendingCoverage()
        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        val flush = inMemoryBuffer.flush().toList()
        //assert on none-empty buffer when coverageTransport.isAvailable() set to `false`
        assertTrue(flush.isNotEmpty())
        assertEquals(3, flush.size)
    }

    @Test
    fun `buffer turn on sending onAvailable`() = runBlocking {
        coverageTransport.onUnavailable()
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        coverageTransport.onAvailable()
        delay(2600)
        verify(sender, times(4)).send(any(), any())
    }

    @Test
    fun `turn on buffer data sending`() = runBlocking {
        coverageTransport.onAvailable()
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        verify(sender, times(3)).send(any(), any())

        coverageTransport.onUnavailable()
        delay(2500)
        val flush = inMemoryBuffer.flush().toList()
        //assert on none-empty buffer when coverageTransport.isAvailable() set to `false`
        assertTrue(flush.isNotEmpty())
        assertEquals(1, flush.size)
    }
}
