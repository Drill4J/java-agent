package com.epam.drill.plugins.test2code.coverage

import com.epam.drill.jacoco.AgentProbes
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

    fun ExecData.fillExecData(sessionId: String = GLOBAL_SESSION_ID, testId: String = "", testName: String = "")
}