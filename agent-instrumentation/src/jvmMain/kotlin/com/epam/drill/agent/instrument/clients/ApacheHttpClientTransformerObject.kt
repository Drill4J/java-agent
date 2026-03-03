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
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_APACHE_HTTP_CLIENT_ENABLED

/**
 * Transformer for Apache HTTP client

 * Tested with:
 *     org.apache.httpcomponents:httpclient:4.2.6
 *     org.apache.httpcomponents:httpclient:4.3.6
 *     org.apache.httpcomponents:httpclient:4.4.1
 *     org.apache.httpcomponents:httpclient:4.5.14
 *     org.apache.httpcomponents:httpclient:5.3.1
 */
abstract class ApacheHttpClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractPropagationTransformer(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_APACHE_HTTP_CLIENT_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        interfaces.any("org/apache/http/HttpClientConnection"::equals) ||
                interfaces.any("org/apache/hc/core5/http/io/HttpClientConnection"::equals)

    override fun transform(className: String, ctClass: CtClass) {
        if (ctClass.isInterface) return
        ctClass.getDeclaredMethod("sendRequestHeader").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isProcessRequests.name}() && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) { 
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    $1.setHeader((String) entry.getKey(), (String) entry.getValue());
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("receiveResponseEntity").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isProcessResponses.name}()) {
                java.util.Map allHeaders = new java.util.HashMap();
                java.util.Iterator iterator = $1.headerIterator();
                while (iterator.hasNext()) {
                    org.apache.http.Header header = (org.apache.http.Header) iterator.next();
                    allHeaders.put(header.getName(), header.getValue());
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
    }

}
