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
package com.epam.drill.agent.test2code.coverage

import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging

/**
 * Descriptor of class probes
 * @param id a class ID
 * @param name a full class name
 * @param probeCount a number of probes in the class
 */
data class ClassDescriptor(
    val id: ClassId,
    val name: String,
    val probeCount: Int,
)

interface IClassDescriptorsProvider {
    fun get(classId: ClassId): ClassDescriptor
}

interface IClassDescriptorStorage {
    fun add(descriptor: ClassDescriptor)
}

interface IClassDescriptorsManager: IClassDescriptorsProvider, IClassDescriptorStorage

class ConcurrentClassDescriptorsManager: IClassDescriptorsManager {
    private val logger = KotlinLogging.logger("${this.javaClass.`package`}.${this.javaClass.name}")

    private val classDescriptors = ConcurrentHashMap<ClassId, ClassDescriptor>()

    override fun add(descriptor: ClassDescriptor) {
        classDescriptors[descriptor.id] = descriptor
    }

    override fun get(classId: ClassId): ClassDescriptor {
        val descriptor = classDescriptors[classId]
        if (descriptor == null) logger.error { "Descriptor for class not found. classId: $$classId" }
        return descriptor!!
    }

}
