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

import com.epam.drill.agent.CHILD_CREATE
import com.epam.drill.agent.PARENT_CREATE_MODIFIED_IN_CHILD
import com.epam.drill.agent.copyTtlValues
import com.epam.drill.agent.ttl.TransmittableThreadLocal
import mu.KotlinLogging
import java.util.concurrent.ConcurrentMap

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
class FooTask(
    private val value: String,
    private val ttlInstances: ConcurrentMap<String, TransmittableThreadLocal<FooPojo>>
) : Runnable {
    private val logger = KotlinLogging.logger {}

    @Volatile
    lateinit var copied: Map<String, FooPojo>

    override fun run() {
        try {
            // Add new
            val child = DeepCopyFooTransmittableThreadLocal()
            child.set(FooPojo(CHILD_CREATE + value, 3))
            ttlInstances[CHILD_CREATE + value] = child

            // modify the parent key
            ttlInstances[PARENT_CREATE_MODIFIED_IN_CHILD]!!.get()!!.name =
                ttlInstances[PARENT_CREATE_MODIFIED_IN_CHILD]!!.get()!!.name + value

            copied = copyTtlValues(ttlInstances)

            logger.info { "Task $value finished!" }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }
}
