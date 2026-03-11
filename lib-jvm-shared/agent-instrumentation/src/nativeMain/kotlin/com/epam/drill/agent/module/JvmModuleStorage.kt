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
package com.epam.drill.agent.module

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentHashMapOf
import com.epam.drill.agent.common.module.AgentModule

actual object JvmModuleStorage {

    private val storage = atomic(persistentHashMapOf<String, AgentModule>())

    actual operator fun get(id: String) = storage.value.get(id)

    actual fun values(): Collection<AgentModule> = storage.value.values

    actual fun add(module: AgentModule) = storage.update { it.put(module.id, module) }

}
