package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class GlobalCoverageRecorderTest {

    @Test
    fun `when recorder is disabled, getCurrent must return null`() {
        val recorder = GlobalCoverageRecorder(enabled = false)
        assertNull(recorder.getCurrent())
    }

    @Test
    fun `after startRecording, getCurrent must not return null`() {
        val recorder = GlobalCoverageRecorder()

        recorder.startRecording("some-session", "some-test")

        assertNotNull(recorder.getCurrent())
    }

    @Test
    fun `after stopRecording, getCurrent must return null`() {
        val recorder = GlobalCoverageRecorder(enabled = true)

        recorder.stopRecording("some-session", "some-test")

        assertNull(recorder.getCurrent())
    }

    @Test
    fun `given exec data without covered probes, poll must return empty`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getCurrent()?.putProbes(123L, false, false, false)

        val result = recorder.pollRecorded()

        assertTrue(result.toList().isEmpty())
    }

    @Test
    fun `given exec data with covered probes, poll must return these probes`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getCurrent()?.putProbes(100L, true)
        recorder.getCurrent()?.putProbes(200L, true, true)
        recorder.getCurrent()?.putProbes(300L, true, true, true)

        val result = recorder.pollRecorded()

        assertEquals(3, result.toList().size)
        assertTrue(result.any { it.probesEquals(100L, true) })
        assertTrue(result.any { it.probesEquals(200L, true, true) })
        assertTrue(result.any { it.probesEquals(300L, true, true, true) })
    }

    @Test
    fun `if there have been no new covered probes since last poll, poll must return empty`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getCurrent()?.putProbes(123L, true, true, true)

        recorder.pollRecorded()
        recorder.getCurrent()?.putProbes(123L, true, false, false)
        val result = recorder.pollRecorded()

        assertTrue(result.toList().isEmpty())
    }

    @Test
    fun `poll must return only new probes that covered since last poll`() {
        val recorder = GlobalCoverageRecorder()
        recorder.getCurrent()?.putProbes(100L, false)
        recorder.getCurrent()?.putProbes(200L, true, false)
        recorder.getCurrent()?.putProbes(300L, true, false, false)

        recorder.pollRecorded()
        recorder.getCurrent()?.putProbes(100L, true)
        recorder.getCurrent()?.putProbes(200L, false, true)
        recorder.getCurrent()?.putProbes(300L, false, true, true)
        val result = recorder.pollRecorded()

        assertTrue(result.any { it.probesEquals(100L, true) })
        assertTrue(result.any { it.probesEquals(200L, false, true) })
        assertTrue(result.any { it.probesEquals(300L, false, true, true) })
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