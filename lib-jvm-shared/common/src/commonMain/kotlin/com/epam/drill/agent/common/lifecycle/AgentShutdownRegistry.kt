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
package com.epam.drill.agent.common.lifecycle

/**
 * Registry for JVM shutdown tasks. Tasks run before the message transport is flushed.
 * [remainingMs] is the time left within the global [shutdownFlushTimeoutMs] budget.
 */
object AgentShutdownRegistry {
    private val tasks = linkedMapOf<String, (Long) -> Unit>()

    fun register(name: String, action: (remainingMs: Long) -> Unit) {
        tasks[name] = action
    }

    fun tasks(): List<NamedShutdownTask> =
        tasks.map { (name, action) -> NamedShutdownTask(name, action) }
}

data class NamedShutdownTask(
    val name: String,
    val action: (remainingMs: Long) -> Unit,
)
