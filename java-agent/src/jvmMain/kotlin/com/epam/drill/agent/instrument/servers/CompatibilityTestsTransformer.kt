package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.test.CompatibilityTestsTransformerObject
import com.epam.drill.agent.request.DrillRequestHolder

actual object CompatibilityTestsTransformer :
    TransformerObject,
    com.epam.drill.common.agent.request.RequestHolder by DrillRequestHolder,
    CompatibilityTestsTransformerObject(),
    ClassPathProvider by RuntimeClassPathProvider