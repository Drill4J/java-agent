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
package com.epam.drill.agent.ttl.threadpool

import com.epam.drill.agent.*
import com.epam.drill.agent.ttl.TtlRunnable
import com.epam.drill.agent.ttl.testmodel.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.*

private const val POOL_SIZE = 3

val threadFactory = ThreadFactory { Thread(it).apply { isDaemon = true } }

val executorService = ThreadPoolExecutor(
    POOL_SIZE, POOL_SIZE,
    10L, TimeUnit.SECONDS,
    LinkedBlockingQueue(), threadFactory
)

val scheduledExecutorService = ScheduledThreadPoolExecutor(POOL_SIZE, threadFactory)

class ExecutorClassesTest {
    @Test
    fun checkThreadPoolExecutorForRemoveMethod() {
        val futures = (0 until POOL_SIZE * 2).map {
            executorService.submit { Thread.sleep(10) }
        }

        Runnable {
            println("Task should be removed!")
        }.let {
            if (noTtlAgentRun()) TtlRunnable.get(it) else it
        }.let {
            executorService.execute(it)
            // Does ThreadPoolExecutor#remove method take effect?
            assertTrue(executorService.remove(it))
            assertFalse(executorService.remove(it))
        }

        // wait sleep task finished.
        futures.forEach { it.get(1, TimeUnit.SECONDS) }
    }

    @Test
    fun checkScheduledExecutorService() {
        val ttlInstances = createParentTtlInstances(ConcurrentHashMap())

        val tag = "2"
        val task = Task(tag, ttlInstances)
        val future = scheduledExecutorService.schedule(
            if (noTtlAgentRun()) TtlRunnable.get(task) else task,
            10,
            TimeUnit.MILLISECONDS
        )

        // create after new Task, won't see parent value in in task!
        createParentTtlInstancesAfterCreateChild(ttlInstances)


        future.get(1, TimeUnit.SECONDS)


        // child Inheritable
        assertChildTtlValues(tag, task.copied)
        // child do not affect parent
        assertParentTtlValues(copyTtlValues(ttlInstances))
    }
}
