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
package com.epam.drill.agent.transport

import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SimpleExponentialBackoffTest {

    @Test
    fun `given stable operation, tryWithExponentialBackoff should end in success on first attempt`() {
        val backoff = SimpleExponentialBackoff()
        val operation = { _: Int, _: Long -> true }
        val result = backoff.tryWithExponentialBackoff(
            onSleep = {},
            operation = operation
        )
        assertTrue(result)
    }

    @Test
    fun `given unstable operation, tryWithExponentialBackoff should retry operation until success`() {
        val backoff = SimpleExponentialBackoff()
        var attemptCount = 0
        val operation = { _: Int, _: Long -> ++attemptCount == 3 }
        val result = backoff.tryWithExponentialBackoff(
            maxRetries = 5,
            onSleep = {},
            operation = operation
        )
        assertTrue(result)
    }

    @Test
    fun `given broken operation, tryWithExponentialBackoff should end with negative result`() {
        val backoff = SimpleExponentialBackoff()
        var attemptCount = 0
        val operation = { _: Int, _: Long -> ++attemptCount; false }
        val result = backoff.tryWithExponentialBackoff(
            maxRetries = 5,
            onSleep = {},
            operation = operation
        )
        assertFalse(result)
    }

    @Test
    fun `given maxDelay, tryWithExponentialBackoff shouldn't exceed it`() {
        val backoff = SimpleExponentialBackoff()
        var maxDelay = 0L
        val operation = { _: Int, _: Long -> false }
        backoff.tryWithExponentialBackoff(
            initDelay = 0L,
            baseDelay = 1000L,
            maxDelay = 5000L,
            factor = 2.0,
            maxRetries = 5,
            onSleep = {
                maxDelay = max(maxDelay, it)
            },
            operation = operation
        )
        assertTrue(maxDelay <= 5000L)
    }

    @Test
    fun `given maxRetries, tryWithExponentialBackoff shouldn't exceed it`() {
        val backoff = SimpleExponentialBackoff()
        var attemptCount = 0
        val operation = { _: Int, _: Long -> ++attemptCount; false }
        val result = backoff.tryWithExponentialBackoff(
            maxRetries = 5,
            onSleep = {},
            operation = operation
        )
        assertTrue(attemptCount <= 5)
    }
}