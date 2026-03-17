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
@file:Suppress("PackageDirectoryMismatch")

// Change the package out of com.epam.drill.agent.ttl
// so agent will transform MyThreadPoolExecutor
package com.epam.drill.agent.ttl.threadpool

import com.epam.drill.agent.hasTtlAgentRun
import com.epam.drill.agent.noTtlAgentRun
import com.epam.drill.agent.ttl.TtlRunnable
import com.epam.drill.agent.ttl.threadpool.TtlExecutors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.*

class MyThreadPoolExecutor(count: Int) : ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, LinkedBlockingQueue()) {
    val runnableList = CopyOnWriteArrayList<Runnable>()
    private val countDownLatch = CountDownLatch(count * 2)

    override fun afterExecute(r: Runnable, t: Throwable?) {
        runnableList.add(r)
        countDownLatch.countDown()
        super.afterExecute(r, t)
    }

    override fun beforeExecute(t: Thread, r: Runnable) {
        runnableList.add(r)
        countDownLatch.countDown()
        super.beforeExecute(t, r)
    }

    fun await() {
        countDownLatch.await()
    }
}

class MyRunnable : Runnable {
    override fun run() {
        Thread.sleep(1)
    }
}

class BeforeAndAfterExecuteMethodOfExecutorSubclassTest {
    private val count = 10

    @Test
    fun underAgent() {
        if (noTtlAgentRun()) return

        val myThreadPoolExecutor = MyThreadPoolExecutor(count)

        (0 until count).map {
            myThreadPoolExecutor.execute(MyRunnable())
        }

        myThreadPoolExecutor.await()

        assertEquals(count * 2, myThreadPoolExecutor.runnableList.size)
        assertTrue(myThreadPoolExecutor.runnableList.all { it is MyRunnable })
    }

    /**
     * for bug submitted by
     * https://github.com/alibaba/transmittable-thread-local/issues/133#issuecomment-1068793261
     */
    @Test
    fun underAgent_task_is_explicit_TtlRunnable__should_not_be_unwrapped() {
        if (noTtlAgentRun()) return

        val myThreadPoolExecutor = MyThreadPoolExecutor(count)

        (0 until count).map {
            val r = TtlRunnable.get(MyRunnable())!!
            myThreadPoolExecutor.execute(r)
        }

        myThreadPoolExecutor.await()

        assertEquals(count * 2, myThreadPoolExecutor.runnableList.size)
        assertTrue(myThreadPoolExecutor.runnableList.all { it is TtlRunnable })
    }

    @Test
    fun noAgent_task_is_TtlRunnable() {
        if (hasTtlAgentRun()) return

        val myThreadPoolExecutor = MyThreadPoolExecutor(count)
        val ttlExecutorService = TtlExecutors.getTtlExecutorService(myThreadPoolExecutor)!!

        (0 until count).map {
            ttlExecutorService.execute(MyRunnable())
        }

        myThreadPoolExecutor.await()

        assertEquals(count * 2, myThreadPoolExecutor.runnableList.size)
        assertTrue(myThreadPoolExecutor.runnableList.all { it is TtlRunnable })
    }

    @Test
    fun noAgent_task_is_NOT_TtlRunnable() {
        if (hasTtlAgentRun()) return

        val myThreadPoolExecutor = MyThreadPoolExecutor(count)

        (0 until count).map {
            myThreadPoolExecutor.execute(MyRunnable())
        }

        myThreadPoolExecutor.await()

        assertEquals(count * 2, myThreadPoolExecutor.runnableList.size)
        assertTrue(myThreadPoolExecutor.runnableList.all { it is MyRunnable })
    }
}
