package com.epam.drill.test2code.coverage

import org.junit.jupiter.api.*

class InMemoryRetentionQueueTest {

    private lateinit var retentionQueue: InMemoryRetentionQueue

    @BeforeEach
    fun setUp() {
        retentionQueue = InMemoryRetentionQueue(totalSizeByteLimit = 10)
    }

    @Test
    fun `multiple elements over limit must cause`() {
        val arraySequence = sequenceOf(
            byteArrayOf(1, 1),
            byteArrayOf(1, 1),
            byteArrayOf(1, 1),
            byteArrayOf(1, 1),
            byteArrayOf(1, 1),
            byteArrayOf(1, 1)
        )
        retentionQueue.addAll(arraySequence)

        val flush = retentionQueue.flush()

        Assertions.assertEquals(10, flush.sumOf { it.size })
    }

    @Test
    fun `elements size to exact limit`() {
        val arraySequence = sequenceOf(
            byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        )
        retentionQueue.addAll(arraySequence)

        val flush = retentionQueue.flush()

        Assertions.assertEquals(10, flush.sumOf { it.size })
    }

    @Test
    fun `singular big element`() {
        val arraySequence = sequenceOf(
            byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        )
        retentionQueue.addAll(arraySequence)

        val flush = retentionQueue.flush()

        Assertions.assertTrue(flush.toList().isEmpty())
    }

    @Test
    fun `flush must return the exact same elements as inserted`() {
        val arraySequence = sequenceOf(
            byteArrayOf(1, 1, 1, 1)
        )
        retentionQueue.addAll(arraySequence)

        val flush = retentionQueue.flush()

        Assertions.assertEquals(4, flush.sumOf { it.size })
    }

    @Test
    fun `queue after flush must be empty`() {
        val arraySequence = sequenceOf(
            byteArrayOf(1, 1, 1, 1, 1)
        )
        retentionQueue.addAll(arraySequence)

        val flush1 = retentionQueue.flush().toList()
        Assertions.assertTrue(flush1.isNotEmpty())

        val flush2 = retentionQueue.flush().toList()
        Assertions.assertTrue(flush2.isEmpty())
    }
}