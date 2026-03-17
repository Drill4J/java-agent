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
package com.epam.drill.agent.ttl

import com.epam.drill.agent.ttl.TtlTimerTask
import io.kotest.core.spec.style.AnnotationSpec
import org.junit.Assert.*
import java.util.*

@Suppress("DEPRECATION")
class TtlTimerTaskTest : AnnotationSpec() {
    @Test
    fun test_get() {
        assertNull(TtlTimerTask.get(null))

        val timerTask = object : TimerTask() {
            override fun run() {}
        }

        val ttlTimerTask = TtlTimerTask.get(timerTask)
        assertTrue(ttlTimerTask is TtlTimerTask)
    }

    @Test
    fun test_unwrap() {
        assertNull(TtlTimerTask.unwrap(null))

        val timerTask = object : TimerTask() {
            override fun run() {}
        }
        val ttlTimerTask = TtlTimerTask.get(timerTask)


        assertSame(timerTask, TtlTimerTask.unwrap(timerTask))
        assertSame(timerTask, TtlTimerTask.unwrap(ttlTimerTask))


        assertEquals(listOf(timerTask), TtlTimerTask.unwraps(listOf(timerTask)))
        assertEquals(listOf(timerTask), TtlTimerTask.unwraps(listOf(ttlTimerTask)))
        assertEquals(listOf(timerTask, timerTask), TtlTimerTask.unwraps(listOf(ttlTimerTask, timerTask)))
        assertEquals(listOf<TimerTask>(), TtlTimerTask.unwraps(null))
    }
}
