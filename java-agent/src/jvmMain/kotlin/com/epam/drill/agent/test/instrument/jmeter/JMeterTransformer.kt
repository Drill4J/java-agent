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
package com.epam.drill.agent.test.instrument.jmeter

import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_JMETER_ENABLED
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.test.instrument.AbstractTestTransformerObject
import javassist.*
import mu.KotlinLogging
import java.security.*

actual object JMeterTransformer : TransformerObject, AbstractTestTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun enabled() = super<AbstractTestTransformerObject>.enabled() && agentConfiguration.parameters[INSTRUMENTATION_JMETER_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/apache/jmeter/protocol/http/sampler/HTTPHC4Impl"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ) {
        val setupRequestMethod = ctClass.getMethod(
            "setupRequest",
            "(Ljava/net/URL;Lorg/apache/http/client/methods/HttpRequestBase;" +
                    "Lorg/apache/jmeter/protocol/http/sampler/HTTPSampleResult;)V"
        )
        //TODO memorizeTestName is not exist
        setupRequestMethod.insertBefore(
            """
                String drillTestName = $3.getSampleLabel();
                AgentClassTransformer.memorizeTestName(drillTestName);
                
                """.trimIndent()
        )
    }
}
