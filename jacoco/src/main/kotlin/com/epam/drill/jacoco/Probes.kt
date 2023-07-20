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
package com.epam.drill.jacoco

open class AgentProbes(
    initialSize: Int = 0,
    val values: BooleanArray = BooleanArray(initialSize),
) {

    open fun set(index: Int) {
        if (!values[index])
            values[index] = true
    }

    fun get(index: Int): Boolean {
        return values[index]
    }

    fun reset() {
        (values.indices).forEach {
            values[it] = false
        }
    }
}

class StubAgentProbes(size: Int = 0) : AgentProbes(size) {
    override fun set(index: Int) {
    }

}
