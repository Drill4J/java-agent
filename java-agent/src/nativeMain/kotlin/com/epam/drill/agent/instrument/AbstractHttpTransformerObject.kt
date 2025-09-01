package com.epam.drill.agent.instrument

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_HTTP_ENABLED

abstract class AbstractHttpTransformerObject: AbstractTransformerObject() {
    override fun enabled() = super<AbstractTransformerObject>.enabled() && Configuration.parameters[INSTRUMENTATION_HTTP_ENABLED]
}