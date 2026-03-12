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
package com.epam.drill.agent.instrument.tomcat

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class TomcatWsClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        "org/apache/tomcat/websocket/WsWebSocketContainer" == className

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting TomcatWsClientTransformer..." }
        val signatures = sequenceOf(
            "(Ljava/lang/String;IZLjakarta/websocket/ClientEndpointConfig;)Ljava/util/Map;",
            "(Ljava/lang/String;IZLjavax/websocket/ClientEndpointConfig;)Ljava/util/Map;",
            "(Ljava/lang/String;ILjavax/websocket/ClientEndpointConfig;)Ljava/util/Map;"
        )
        val getMethod: (String) -> CtMethod? = {
            ctClass
                .runCatching { this.getMethod("createRequestHeaders", it) }
                .getOrNull()
        }
        signatures.mapNotNull(getMethod).first().insertCatching(
            CtBehavior::insertAfter,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) { 
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    ${'$'}_.put((String) entry.getKey(), java.util.Collections.singletonList((String) entry.getValue()));
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )
    }

}
