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

import com.epam.drill.agent.hasTtlAgentRunWithDisableInheritableForThreadPool
import com.epam.drill.agent.ttl.threadpool.TtlExecutors
import com.epam.drill.agent.ttl.threadpool.TtlForkJoinPoolHelper
import com.epam.drill.agent.ttl.threadpool.agent.TtlAgent
import com.epam.drill.agent.ttl.TransmittableThreadLocal
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadPoolExecutor

private const val hello = "hello"
private val defaultValue = "${Date()} ${Math.random()}"

class InheritableTest : AnnotationSpec() {

    // ===================================================
    // Executors
    // ===================================================

    @Test
    fun inheritable_Executors() {
        val threadPool = Executors.newCachedThreadPool()
        try {
            val ttl = TransmittableThreadLocal<String?>()
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            // get "hello" value is transmitted by InheritableThreadLocal function!
            // NOTE: Executors.newCachedThreadPool create thread lazily

            threadPool.submit(callable).get() shouldBe hello

            // current thread's TTL must be existed
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_Executors_DisableInheritableThreadFactory() {
        val threadPool = Executors.newCachedThreadPool(TtlExecutors.getDefaultDisableInheritableThreadFactory())
        try {
            val ttl = TransmittableThreadLocal<String?>()
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get().shouldBeNull()
            }


            // current thread's TTL must be existed when using DisableInheritableThreadFactory
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_Executors_TtlDisableInheritableWithInitialValue() {
        val threadPool = Executors.newCachedThreadPool()
        try {
            val ttl = object : TransmittableThreadLocal<String?>() {
                override fun childValue(parentValue: String?): String? = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!


            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get().shouldBeNull()
            }

            // current thread's TTL must be existed
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_Executors_TtlDefaultValue_TtlDisableInheritableWithInitialValue() {
        val threadPool = Executors.newCachedThreadPool()
        try {
            val ttl = object : TransmittableThreadLocal<String>() {
                override fun initialValue(): String = defaultValue
                override fun childValue(parentValue: String): String = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!


            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: defaultValue
                threadPool.submit(callable).get() shouldBe defaultValue
            }

            // current thread's TTL must be existed when using DisableInheritableThreadFactory
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_Executors_TtlDefaultValue_DisableInheritableThreadFactory_TtlWithInitialValue() {
        val threadPool = Executors.newCachedThreadPool(TtlExecutors.getDefaultDisableInheritableThreadFactory())
        try {
            val ttl = object : TransmittableThreadLocal<String>() {
                override fun initialValue(): String = defaultValue
                override fun childValue(parentValue: String): String = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: defaultValue
                threadPool.submit(callable).get() shouldBe defaultValue
            }

            // current thread's TTL must be existed when using DisableInheritableThreadFactory
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_Executors_ByAgent() {
        val threadPool = Executors.newCachedThreadPool() as ThreadPoolExecutor
        try {
            TtlExecutors.isDisableInheritableThreadFactory(threadPool.threadFactory) shouldBe
                    hasTtlAgentRunWithDisableInheritableForThreadPool()

        } finally {
            threadPool.shutdown()
        }
    }

    // ===================================================
    // ForkJoinPool
    // ===================================================

    @Test
    fun inheritable_ForkJoinPool() {
        val threadPool = ForkJoinPool(4)
        try {
            val ttl = TransmittableThreadLocal<String?>()
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            // get "hello" value is transmitted by InheritableThreadLocal function!
            // NOTE: Executors.newCachedThreadPool create thread lazily
            threadPool.submit(callable).get() shouldBe hello

            // current thread's TTL must be existed
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_ForkJoinPool_DisableInheritableForkJoinWorkerThreadFactory() {
        val threadPool = ForkJoinPool(
            4,
            TtlForkJoinPoolHelper.getDefaultDisableInheritableForkJoinWorkerThreadFactory(),
            null,
            false
        )
        try {
            val ttl = TransmittableThreadLocal<String?>()
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get().shouldBeNull()
            }

            // current thread's TTL must be existed when using DisableInheritableForkJoinWorkerThreadFactory
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_ForkJoinPool_TtlDisableInheritableWithInitialValue() {
        val threadPool = ForkJoinPool(4)
        try {
            val ttl = object : TransmittableThreadLocal<String?>() {
                override fun childValue(parentValue: String?): String? = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get().shouldBeNull()
            }

            // current thread's TTL must be existed
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_ForkJoinPool_TtlDefaultValue_TtlDisableInheritableWithInitialValue() {
        val threadPool = ForkJoinPool(4)
        try {
            val ttl = object : TransmittableThreadLocal<String>() {
                override fun initialValue(): String = defaultValue
                override fun childValue(parentValue: String): String = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!
            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get() shouldBe defaultValue
            }

            // current thread's TTL must be existed
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_ForkJoinPool_TtlDefaultValue_DisableInheritableForkJoinWorkerThreadFactory_TtlWithInitialValue() {
        val threadPool = ForkJoinPool(
            4,
            TtlForkJoinPoolHelper.getDefaultDisableInheritableForkJoinWorkerThreadFactory(),
            null,
            false
        )
        try {
            val ttl = object : TransmittableThreadLocal<String>() {
                override fun initialValue(): String = defaultValue
                override fun childValue(parentValue: String): String = initialValue()
            }
            ttl.set(hello)

            val callable = Callable { ttl.get() } // NO TtlWrapper(TtlCallable) here!!

            if (TtlAgent.isTtlAgentLoaded()) {
                // when ttl agent is loaded, Callable is wrapped when submitted,
                // so here value is "hello" transmitted by TtlCallable wrapper
                threadPool.submit(callable).get() shouldBe hello
            } else {
                // when ttl agent is not loaded: null
                threadPool.submit(callable).get() shouldBe defaultValue
            }

            // current thread's TTL must be existed when using DisableInheritableForkJoinWorkerThreadFactory
            ttl.get() shouldBe hello
        } finally {
            threadPool.shutdown()
        }
    }

    @Test
    fun disableInheritable_ForkJoinPool_ByAgent() {
        val threadPool = ForkJoinPool(4)
        try {
            TtlForkJoinPoolHelper.isDisableInheritableForkJoinWorkerThreadFactory(threadPool.factory) shouldBe
                    hasTtlAgentRunWithDisableInheritableForThreadPool()
        } finally {
            threadPool.shutdown()
        }
    }

}
