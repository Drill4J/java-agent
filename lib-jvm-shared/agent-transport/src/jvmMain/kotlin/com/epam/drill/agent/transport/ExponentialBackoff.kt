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

/**
 * Interface for retrying an operation with exponential backoff.
 */
interface ExponentialBackoff {
    /**
     * Retries an operation with exponential backoff.
     * @param initDelay Initial delay in milliseconds before the first retry. Default is 0ms (no delay).
     * @param baseDelay Initial delay in milliseconds between retries. Default is 1000ms (1 second).
     * @param maxDelay Maximum delay in milliseconds between retries. Default is 32000ms (32 seconds).
     * @param factor Multiplier for increasing the delay after each failed attempt. Default is 2.0 (doubling the delay).
     * @param maxRetries Maximum number of retry attempts before giving up. Default is 5.
     * @param operation A lambda function representing the operation to be executed.
     *                  It should return `true` if successful, or `false` if it failed.
     * @return `true` if the operation was successful, `false` if all attempts were exhausted.
     */
    fun tryWithExponentialBackoff(
        initDelay: Long = 0L,
        baseDelay: Long = 1000L,
        maxDelay: Long = 32000L,
        factor: Double = 2.0,
        maxRetries: Int = 5,
        onSleep: (Long) -> Unit = { Thread.sleep(it) },
        operation: (Int, Long) -> Boolean
    ): Boolean
}