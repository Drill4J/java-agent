package com.epam.drill.agent.instrument

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_ENABLED

abstract class AbstractTransformerObject: JvmTransformerObject() {
    override fun enabled() = Configuration.parameters[INSTRUMENTATION_ENABLED]
}