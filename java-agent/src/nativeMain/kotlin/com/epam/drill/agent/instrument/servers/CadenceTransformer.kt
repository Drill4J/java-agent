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
package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.JvmTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.instrument.CADENCE_CONSUMER
import com.epam.drill.agent.instrument.CADENCE_PRODUCER
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_CADENCE_ENABLED


actual object CadenceTransformer : TransformerObject, AbstractTransformerObject() {
    override fun enabled() = super<AbstractTransformerObject>.enabled() && Configuration.parameters[INSTRUMENTATION_CADENCE_ENABLED]
    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        CADENCE_PRODUCER == className || CADENCE_CONSUMER == className
}
