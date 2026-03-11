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
package com.epam.drill.agent.common.request

object DrillInitialContext {
    private val context: MutableMap<String, String> = mutableMapOf()

    fun add(key: String, value: String) {
        context[key] = value
    }

    fun remove(key: String) {
        context.remove(key)
    }

    fun get(key: String): String? = context[key]

    fun getAll(): Map<String, String> = context.toMap()

    fun getDrillRequest(): DrillRequest? {
        return get("drill-session-id").takeIf { !it.isNullOrBlank() }?.let { testSessionId ->
            DrillRequest(
                drillSessionId = testSessionId,
                headers = getAll().filter { it.key != "drill-session-id" }.toMap(),
            )
        }
    }
}