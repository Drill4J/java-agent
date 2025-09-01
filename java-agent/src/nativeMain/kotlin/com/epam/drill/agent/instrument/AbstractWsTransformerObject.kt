package com.epam.drill.agent.instrument

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_WS_ENABLED

abstract class AbstractWsTransformerObject: AbstractTransformerObject() {
    override fun enabled() = super<AbstractTransformerObject>.enabled() && Configuration.parameters[INSTRUMENTATION_WS_ENABLED]
}