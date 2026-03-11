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
package com.epam.drill.agent.instrument.clients

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.instrument.AbstractPropagationTransformer
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_SPRING_WEB_CLIENT_ENABLED
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

/**
 * Transformer for Spring Webflux WebClient
 */
abstract class SpringWebClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractPropagationTransformer(agentConfiguration) {
    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_SPRING_WEB_CLIENT_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        interfaces.any("org/springframework/web/reactive/function/client/ClientRequest"::equals)

    override fun transform(className: String, ctClass: CtClass) {
        if (ctClass.isInterface) return
        ctClass.getDeclaredMethod("writeTo").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isProcessRequests.name}() && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {                
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    $1.getHeaders().add((String) entry.getKey(), (String) entry.getValue());
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )
    }
}