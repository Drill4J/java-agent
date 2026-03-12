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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
class Task(
    private val tag: String,
    private val ttlInstances: ConcurrentMap<String, TransmittableThreadLocal<String>> = ConcurrentHashMap()
) : Runnable {

    private val queue = ArrayBlockingQueue<Map<String, String>>(1)

    val copied: Map<String, String>
        get() = queue.poll(1, TimeUnit.SECONDS)!!

    override fun run() {
        val map = createChildTtlInstancesAndModifyParentTtlInstances(tag, ttlInstances)
        queue.put(map)
    }
}
