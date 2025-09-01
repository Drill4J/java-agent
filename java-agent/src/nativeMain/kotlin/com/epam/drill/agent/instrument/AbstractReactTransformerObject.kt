package com.epam.drill.agent.instrument

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_REACTOR_ENABLED

abstract class AbstractReactTransformerObject: AbstractTransformerObject() {
    override fun enabled() = super<AbstractTransformerObject>.enabled() && Configuration.parameters[INSTRUMENTATION_REACTOR_ENABLED]
}