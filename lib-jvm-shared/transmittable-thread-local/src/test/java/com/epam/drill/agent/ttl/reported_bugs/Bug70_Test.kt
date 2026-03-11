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
package com.epam.drill.agent.ttl.reported_bugs

import com.epam.drill.agent.noTtlAgentRun
import com.epam.drill.agent.ttl.TransmittableThreadLocal
import com.epam.drill.agent.ttl.TtlRunnable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Bug URL: https://github.com/alibaba/transmittable-thread-local/issues/70
 * Reporter: @aftersss
 */
class Bug70_Test {

    @Test
    fun test_bug70() {
        val hello = "hello"
        val executorService = Executors.newSingleThreadExecutor()
        val threadLocal = TransmittableThreadLocal<String>().apply { set(hello) }
        assertEquals(hello, threadLocal.get())

        FutureTask { threadLocal.get() }.also {
            val runnable = if (noTtlAgentRun()) TtlRunnable.get(it) else it
            executorService.submit(runnable)
            assertEquals(hello, it.get())
        }

        val taskRef = AtomicReference<FutureTask<String>>()
        thread(name = "the thread for run executor action") {
            FutureTask { threadLocal.get() }.also {
                val runnable = if (noTtlAgentRun()) TtlRunnable.get(it, false, false) else it
                executorService.submit(runnable)
                taskRef.set(it)
            }
        }.join()
        assertEquals(hello, taskRef.get().get())
    }
}
