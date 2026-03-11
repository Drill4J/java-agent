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
package com.epam.drill.agent.ttl.forkjoin

import com.epam.drill.agent.expandThreadPool
import com.epam.drill.agent.ttl.TransmittableThreadLocal
import com.epam.drill.agent.ttl.threadpool.agent.TtlAgent
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.ForkJoinPool


private const val hello = "hello"

class ForkJoinPool4StreamTest : AnnotationSpec() {

    @Test
    fun test_stream_with_agent() {
        if (!TtlAgent.isTtlAgentLoaded()) return

        expandThreadPool(ForkJoinPool.commonPool())

        val ttl = TransmittableThreadLocal<String?>()
        ttl.set(hello)

        (0..100).map {
            ForkJoinPool.commonPool().submit {
                ttl.get() shouldBe hello
            }
        }.forEach { it.get() }

        (0..1000).toList().stream().parallel().mapToInt {
            ttl.get() shouldBe hello

            it
        }.sum() shouldBe (0..1000).sum()
    }

    @Test
    fun test_stream_no_agent() {
        if (TtlAgent.isTtlAgentLoaded()) return

        val name = Thread.currentThread().name
        expandThreadPool(ForkJoinPool.commonPool())

        val ttl = TransmittableThreadLocal<String?>()
        ttl.set(hello)

        (0..100).map {
            ForkJoinPool.commonPool().submit {
                if (Thread.currentThread().name == name) ttl.get() shouldBe hello
                else ttl.get().shouldBeNull()
            }
        }.forEach { it.get() }

        (0..1000).toList().stream().parallel().mapToInt {
            if (Thread.currentThread().name == name) ttl.get() shouldBe hello
            else ttl.get().shouldBeNull()

            it
        }.sum() shouldBe (0..1000).sum()
    }
}
