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
package com.epam.drill.agent.instrument.test

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_COMPATIBILITY_TESTS_ENABLED
import javassist.CtClass
import mu.KotlinLogging

private const val COMPATIBILITY_TEST_CLASS_NAME = "com/epam/drill/compatibility/context/DrillTestContext"
private const val DRILL_SESSION_ID_HEADER = "drill-session-id"

/**
 * Uses for compatibility tests https://github.com/Drill4J/internal-compatibility-matrix-tests.
 */
abstract class CompatibilityTestsTransformerObject(agentConfiguration: AgentConfiguration) :
    RequestHolder,
    AbstractTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_COMPATIBILITY_TESTS_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        className == COMPATIBILITY_TEST_CLASS_NAME

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getDeclaredMethod("retrieve").setBody(
            """             
                {
                    ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                    if (drillRequest != null) {
                        java.util.Map context = new java.util.HashMap();
                        context.putAll(drillRequest.getHeaders());
                        context.put("$DRILL_SESSION_ID_HEADER", drillRequest.getDrillSessionId());
                        return context;
                    } else {
                        return null;
                    }                                            
                }            
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("store").setBody(
            """
                {
                    java.lang.String sessionId = (java.lang.String) $1.get("$DRILL_SESSION_ID_HEADER");
                    ${DrillRequest::class.java.name} drillRequest = new ${DrillRequest::class.java.name}(sessionId, $1);
                    ${this::class.java.name}.INSTANCE.${RequestHolder::store.name}(drillRequest);                
                }                    
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("remove").setBody(
            """    
                {                            
                    ${this::class.java.name}.INSTANCE.${RequestHolder::remove.name}();
                }                    
            """.trimIndent()
        )
    }

}
