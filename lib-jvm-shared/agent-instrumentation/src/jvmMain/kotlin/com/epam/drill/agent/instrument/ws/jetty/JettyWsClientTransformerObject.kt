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
package com.epam.drill.agent.instrument.jetty

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class JettyWsClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        listOf(
            "org/eclipse/jetty/websocket/client/WebSocketClient",
            "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting JettyWsClientTransformer for $className..." }
        when (className) {
            "org/eclipse/jetty/websocket/client/WebSocketClient" -> transformWebSocketClient(ctClass)
            "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient" -> transformWebSocketCoreClient(ctClass)
        }
    }

    private fun transformWebSocketClient(ctClass: CtClass) = ctClass
        .getMethod(
            "connect",
            "(Ljava/lang/Object;Ljava/net/URI;Lorg/eclipse/jetty/websocket/client/ClientUpgradeRequest;Lorg/eclipse/jetty/websocket/client/io/UpgradeListener;)Ljava/util/concurrent/Future;"
        )
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) { 
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    $3.setHeader((String) entry.getKey(), (String) entry.getValue());
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )

    private fun transformWebSocketCoreClient(ctClass: CtClass) = ctClass
        .getMethod(
            "connect",
            "(Lorg/eclipse/jetty/websocket/core/client/CoreClientUpgradeRequest;)Ljava/util/concurrent/CompletableFuture;"
        )
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) { 
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    $1.addHeader(new org.eclipse.jetty.http.HttpField((String) entry.getKey(), (String) entry.getValue()));
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )

}
