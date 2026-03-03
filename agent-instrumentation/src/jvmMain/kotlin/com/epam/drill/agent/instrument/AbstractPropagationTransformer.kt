package com.epam.drill.agent.instrument

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.CONTEXT_PROPAGATION_ENABLED

abstract class AbstractPropagationTransformer(
    agentConfiguration: AgentConfiguration
) : AbstractTransformerObject(agentConfiguration) {
    override fun enabled(): Boolean {
        return super.enabled() && agentConfiguration.parameters[CONTEXT_PROPAGATION_ENABLED]
    }
}