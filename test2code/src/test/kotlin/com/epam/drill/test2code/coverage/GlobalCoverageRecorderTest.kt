package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class GlobalCoverageRecorderTest {

    @Test
    fun `given exec data without covered probes, pollRecorded must return empty`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getContext().putProbes(123L, false, false, false)

        val result = recorder.pollRecorded()

        assertTrue(result.toList().isEmpty())
    }

    @Test
    fun `given exec data with covered probes, pollRecorded must return these probes`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getContext().putProbes(100L, true)
        recorder.getContext().putProbes(200L, true, true)
        recorder.getContext().putProbes(300L, true, true, true)

        val result = recorder.pollRecorded()

        assertEquals(3, result.toList().size)
        assertTrue(result.any { it.probesEquals(100L, true) })
        assertTrue(result.any { it.probesEquals(200L, true, true) })
        assertTrue(result.any { it.probesEquals(300L, true, true, true) })
    }

    @Test
    fun `if there have new exec data since last poll, pollRecorded must return all exec data`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getContext().putProbes(100L, true)

        recorder.pollRecorded()
        recorder.getContext().putProbes(200L, true, false)
        val result = recorder.pollRecorded()

        assertEquals(2, result.toList().size)
        assertTrue(result.any { it.probesEquals(100L, true) })
        assertTrue(result.any { it.probesEquals(200L, true, false) })
    }

    @Test
    fun `if there have been changes in probes since last poll, pollRecorded must return all exec data`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getContext().putProbes(123L, true, true, false)

        recorder.pollRecorded()
        recorder.getContext().putProbes(123L, true, true, true)
        val result = recorder.pollRecorded()

        assertTrue(result.any { it.probesEquals(123L, true, true, true) })
    }

    @Test
    fun `if there have been no changes in probes since last poll, pollRecorded must return empty`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getContext().putProbes(123L, true, true, false)

        recorder.pollRecorded()
        recorder.getContext().putProbes(123L, true, true, false)
        val result = recorder.pollRecorded()

        assertTrue(result.toList().isEmpty())
    }

    private fun ContextCoverage.putProbes(classId: Long, vararg probes: Boolean) {
        execData[classId] = ExecDatum(
            id = classId,
            name = "foo",
            sessionId = SESSION_CONTEXT_AMBIENT,
            probes = AgentProbes(initialSize = probes.size, values = booleanArrayOf(*probes)),
            testId = TEST_CONTEXT_NONE
        )
    }

    private fun ExecDatum.probesEquals(classId: Long, vararg probes: Boolean): Boolean {
        return this.id == classId && this.probes.values.contentEquals(booleanArrayOf(*probes))
    }
}