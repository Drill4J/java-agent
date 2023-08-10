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
package com.epam.drill.plugins.test2code.coverage

import com.epam.drill.plugins.test2code.common.api.DEFAULT_TEST_NAME

/**
 * Descriptor of class probes
 * @param id a class ID
 * @param name a full class name
 * @param probeCount a number of probes in the class
 */
class ProbeDescriptor(
    val id: ClassId,
    val name: String,
    val probeCount: Int,
)

interface ProbeDescriptorProvider {
    /**
     * Add a new probe descriptor
     */
    fun addProbeDescriptor(descriptor: ProbeDescriptor)

    fun ExecData.fillExecData(
        sessionId: String = GLOBAL_SESSION_ID,
        testId: String = DEFAULT_TEST_ID,
        testName: String = DEFAULT_TEST_NAME
    )
}