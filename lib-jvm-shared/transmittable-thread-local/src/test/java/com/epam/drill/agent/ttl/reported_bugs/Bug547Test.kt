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

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Bug URL: https://github.com/alibaba/transmittable-thread-local/issues/547
 * Reporter: @robin-g-20230331
 */
class Bug547Test : AnnotationSpec() {
    private val scheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(2).apply {
        removeOnCancelPolicy = true
    }

    @Test
    fun test_bug547() {
        scheduledThreadPoolExecutor.queue.size shouldBe 0

        val future = scheduledThreadPoolExecutor.schedule({}, 1, TimeUnit.DAYS)
        scheduledThreadPoolExecutor.queue.size shouldBe 1

        future.cancel(false)

        scheduledThreadPoolExecutor.queue.size shouldBe 0
    }

    @AfterAll
    fun afterAll() {
        scheduledThreadPoolExecutor.shutdown()
    }
}
