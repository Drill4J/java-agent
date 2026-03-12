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
package com.epam.drill.agent.ttl.testmodel

import com.epam.drill.agent.createChildTtlInstancesAndModifyParentTtlInstances
import com.epam.drill.agent.ttl.TransmittableThreadLocal
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
class Call(private val tag: String, private val ttlInstances: ConcurrentMap<String, TransmittableThreadLocal<String>> = ConcurrentHashMap()) : Callable<String> {

    lateinit var copied: Map<String, String>

    val isCopied: Boolean
        get() = ::copied.isInitialized

    override fun call(): String {
        copied = createChildTtlInstancesAndModifyParentTtlInstances(tag, ttlInstances)
        return "ok"
    }
}
