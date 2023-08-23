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
package com.epam.drill.test2code.coverage

import java.util.concurrent.ConcurrentHashMap

/**
 * Descriptor of class probes
 * @param id a class ID
 * @param name a full class name
 * @param probeCount a number of probes in the class
 */
class ProbesDescriptor(
    val id: ClassId,
    val name: String,
    val probeCount: Int,
)

interface ProbesDescriptorProvider: Iterable<ProbesDescriptor> {
    /**
     * Add a new probe descriptor
     */
    fun addDescriptor(descriptor: ProbesDescriptor)
}

class ConcurrentProbesDescriptorProvider: ProbesDescriptorProvider {

    private val probesDescriptors = ConcurrentHashMap<ClassId, ProbesDescriptor>()

    override fun addDescriptor(descriptor: ProbesDescriptor) {
        probesDescriptors[descriptor.id] = descriptor
    }

    override fun iterator(): Iterator<ProbesDescriptor> = probesDescriptors.values.iterator()

}