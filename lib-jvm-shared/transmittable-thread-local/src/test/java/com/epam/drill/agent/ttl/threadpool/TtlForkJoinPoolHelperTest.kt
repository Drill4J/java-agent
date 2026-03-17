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

import com.epam.drill.agent.ttl.TtlUnwrap
import com.epam.drill.agent.ttl.threadpool.DisableInheritableForkJoinWorkerThreadFactory
import com.epam.drill.agent.ttl.threadpool.TtlForkJoinPoolHelper
import io.kotest.core.spec.style.AnnotationSpec
import org.junit.Assert.*
import java.util.concurrent.ForkJoinPool

class TtlForkJoinPoolHelperTest : AnnotationSpec() {
    @Test
    fun test_DisableInheritableForkJoinWorkerThreadFactory() {
        TtlForkJoinPoolHelper.getDefaultDisableInheritableForkJoinWorkerThreadFactory().let {
            assertTrue(it is DisableInheritableForkJoinWorkerThreadFactory)
            assertTrue(TtlForkJoinPoolHelper.isDisableInheritableForkJoinWorkerThreadFactory(it))

            assertSame(ForkJoinPool.defaultForkJoinWorkerThreadFactory, TtlForkJoinPoolHelper.unwrap(it))
            assertSame(ForkJoinPool.defaultForkJoinWorkerThreadFactory, TtlUnwrap.unwrap(it))
        }
    }

    @Test
    fun test_null() {
        assertFalse(TtlForkJoinPoolHelper.isDisableInheritableForkJoinWorkerThreadFactory(null))
        assertNull(TtlForkJoinPoolHelper.unwrap(null))
    }
}
