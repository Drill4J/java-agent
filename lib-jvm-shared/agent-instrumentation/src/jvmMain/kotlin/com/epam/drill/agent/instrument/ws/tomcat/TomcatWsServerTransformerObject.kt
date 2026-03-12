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
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

/**
 * Transformer for Tomcat based websockets
 *
 * Tested with:
 *     org.apache.tomcat.embed:tomcat-embed-websocket:9.0.83
 */
abstract class TomcatWsServerTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        "org/apache/tomcat/websocket/server/WsHttpUpgradeHandler" == className

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting TomcatWsServerTransformer..." }
        val method = ctClass.getMethod(
            "upgradeDispatch",
            "(Lorg/apache/tomcat/util/net/SocketEvent;)Lorg/apache/tomcat/util/net/AbstractEndpoint\$Handler\$SocketState;"
        )
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1 == org.apache.tomcat.util.net.SocketEvent.OPEN_READ) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = this.handshakeRequest.getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = this.handshakeRequest.getHeaders().get(headerName);
                    java.lang.String header = java.lang.String.join(",", headerValues);
                    allHeaders.put(headerName, header);
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
            }
            """.trimIndent()
        )
        method.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1 == org.apache.tomcat.util.net.SocketEvent.OPEN_READ && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

}
