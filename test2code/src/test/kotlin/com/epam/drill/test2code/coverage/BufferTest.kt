package com.epam.drill.test2code.coverage

import com.epam.drill.common.agent.*
import com.epam.drill.jacoco.*
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*
import java.util.concurrent.atomic.*
import kotlin.random.Random

class BufferTest {

    // General services and vars at test execution
    private lateinit var sender: Sender
    private lateinit var id: String
    private lateinit var coverageSender: CoverageSender
    private lateinit var inMemoryBuffer: InMemoryBuffer
    private lateinit var coverageTransport: CoverageTransport

    @BeforeEach
    fun setUp() {
        id = UUID.randomUUID().toString()
        sender = mock()
        inMemoryBuffer = InMemoryBuffer()
        coverageTransport = spy(CoverageTransportImpl(id, sender))
        coverageSender = IntervalCoverageSender(2500, coverageTransport, inMemoryBuffer) {
            sequenceOf(
                ExecDatum(
                    id = Random.nextLong(),
                    probes = AgentProbes(initialSize = 3, values = booleanArrayOf(true, true, true)),
                    name = "foo/bar"
                )
            )
        }
    }

    @Test
    fun `buffer collect`() = runBlocking {
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
        whenever(coverageTransport.isAvailable()).thenReturn(AtomicBoolean(false))
        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        whenever(coverageTransport.isAvailable()).thenReturn(AtomicBoolean(true))

        delay(2600)
        verify(sender, times(1)).send(eq(id), any())
    }


    @Test
    fun `turn on buffer data sending`() = runBlocking {
        whenever(coverageTransport.isAvailable()).thenReturn(AtomicBoolean(true))

        coverageSender.startSendingCoverage()

        //fill 3 times (intervalMs = 2500, 2500*3 = 7500, so we have to set 8000)
        delay(8000)
        verify(sender, times(3)).send(eq(id), any())

        whenever(coverageTransport.isAvailable()).thenReturn(AtomicBoolean(false))
        delay(2500)
        val flush = inMemoryBuffer.flush().toList()
        //assert on none-empty buffer when coverageTransport.isAvailable() set to `false`
        assertTrue(flush.isNotEmpty())
        assertEquals(1, flush.size)
    }
}
