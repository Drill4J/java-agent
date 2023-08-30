package com.epam.drill.test2code.coverage

import com.epam.drill.common.agent.*
import com.epam.drill.jacoco.*
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.*
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
        inMemoryRetentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = 90)
        coverageTransport = spy(WebsocketCoverageTransport(UUID.randomUUID().toString(), sender))
        coverageSender = IntervalCoverageSender(2500, coverageTransport, inMemoryRetentionQueue) {
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
        inMemoryRetentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = 10)
        coverageSender = IntervalCoverageSender(2500, coverageTransport, inMemoryRetentionQueue) {
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
