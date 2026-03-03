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
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_JAVA_HTTP_CLIENT_ENABLED

/**
 * Transformer for Java HttpURLConnection client

 * Tested with:
 *     jdk 1.8.0_241
 */
abstract class JavaHttpClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractPropagationTransformer(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_JAVA_HTTP_CLIENT_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        "java/net/HttpURLConnection" == superName || "javax/net/ssl/HttpsURLConnection" == superName

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.constructors.forEach {
            it.insertCatching(
                CtBehavior::insertAfter,
                """
                if (${this::class.java.name}.INSTANCE.${this::isProcessRequests.name}() && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                    java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                    java.util.Iterator iterator = headers.entrySet().iterator();                      
                    while (iterator.hasNext()) {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                        this.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
                    }
                    ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);   
                }
                """.trimIndent()
            )
        }
        if (className == "sun/net/www/protocol/http/HttpURLConnection") {
            ctClass.getMethod("getInputStream", "()Ljava/io/InputStream;").insertCatching(
                CtBehavior::insertAfter,
                """
                if (${this::class.java.name}.INSTANCE.${this::isProcessResponses.name}() && this.filteredHeaders == null) {
                    java.util.Map allHeaders = new java.util.HashMap();
                    java.util.Iterator iterator = this.getFilteredHeaderFields().keySet().iterator();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        String value = this.getHeaderField(key);
                        allHeaders.put(key, value);
                    }
                    ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
                }
                """.trimIndent()
            )
        } else {
            ctClass.getMethod("getContent", "()Ljava/lang/Object;").insertCatching(
                CtBehavior::insertAfter,
                """
                if (${this::class.java.name}.INSTANCE.${this::isProcessResponses.name}()) {
                    java.util.Map allHeaders = new java.util.HashMap();
                    java.util.Iterator iterator = this.getHeaderFields().keySet().iterator();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        String value = this.getHeaderField(key);
                        allHeaders.put(key, value);
                    }
                    ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
                }
                """.trimIndent()
            )
        }
    }

}
