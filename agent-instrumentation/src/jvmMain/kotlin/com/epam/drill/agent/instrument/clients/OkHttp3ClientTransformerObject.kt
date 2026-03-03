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
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_OK_HTTP_CLIENT_ENABLED

/**
 * Transformer for OkHttp3 client

 * Tested with:
 *     com.squareup.okhttp3:okhttp:3.12.13
 *     com.squareup.okhttp3:okhttp:3.14.9
 *     com.squareup.okhttp3:okhttp:4.12.0
 */
abstract class OkHttp3ClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractPropagationTransformer(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_OK_HTTP_CLIENT_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        interfaces.any("okhttp3/internal/http/HttpCodec"::equals) ||
                interfaces.any("okhttp3/internal/http/ExchangeCodec"::equals)

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getDeclaredMethod("writeRequestHeaders").insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isProcessRequests.name}() && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                okhttp3.Request.Builder builder = $1.newBuilder();
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    builder.addHeader((String) entry.getKey(), (String) entry.getValue());
                }
                $1 = builder.build();
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);                    
            }
            """.trimIndent()
        )
        val methodName = "openResponseBody".takeIf(ctClass.declaredMethods.map(CtMethod::getName)::contains)
            ?: "openResponseBodySource"
        ctClass.getDeclaredMethod(methodName)
            .insertCatching(
                CtBehavior::insertBefore,
                """
            if (${this::class.java.name}.INSTANCE.${this::isProcessResponses.name}()) {
                java.util.Map allHeaders = new java.util.HashMap();
                java.util.Iterator iterator = $1.headers().names().iterator();
                while (iterator.hasNext()) { 
                    String key = (String) iterator.next();
                    String value = $1.headers().get(key);
                    allHeaders.put(key, value);
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
            )
    }

}
