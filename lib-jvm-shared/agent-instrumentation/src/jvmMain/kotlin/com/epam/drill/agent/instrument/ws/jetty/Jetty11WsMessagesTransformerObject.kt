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
import javassist.CtField
import javassist.CtMethod
import javassist.CtNewMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class Jetty11WsMessagesTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    PayloadProcessor, AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) = listOf(
        "org/eclipse/jetty/websocket/core/FrameHandler",
        "org/eclipse/jetty/websocket/core/internal/WebSocketCoreSession",
        "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient",
        "org/eclipse/jetty/websocket/core/server/internal/CreatorNegotiator",
        "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler",
        "org/eclipse/jetty/websocket/server/internal/JettyServerFrameHandlerFactory",
        "org/eclipse/jetty/websocket/jakarta/common/JakartaWebSocketFrameHandler",
        "org/eclipse/jetty/websocket/jakarta/client/internal/JsrUpgradeListener"
    ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting Jetty11WsMessagesTransformerObject for $className..." }
        try {
            if (className != "org/eclipse/jetty/websocket/core/FrameHandler"
                && className != "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler"
                && className != "org/eclipse/jetty/websocket/jakarta/common/JakartaWebSocketFrameHandler"
            ) {
                ctClass.classPool.classLoader.loadClass("org.eclipse.jetty.websocket.core.FrameHandler")
                ctClass.classPool.classLoader.loadClass("org.eclipse.jetty.websocket.common.JettyWebSocketFrameHandler")
                ctClass.classPool.classLoader.loadClass("org.eclipse.jetty.websocket.jakarta.common.JakartaWebSocketFrameHandler")
            }
            when (className) {
                "org/eclipse/jetty/websocket/core/FrameHandler" -> transformFrameHandler(ctClass)
                "org/eclipse/jetty/websocket/core/internal/WebSocketCoreSession" -> transformCoreSession(ctClass)
                "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient" -> transformWebSocketCoreClient(ctClass)
                "org/eclipse/jetty/websocket/core/server/internal/CreatorNegotiator" -> transformCreatorNegotiator(
                    ctClass
                )

                "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler" -> transformWebSocketFrameHandler(
                    ctClass
                )

                "org/eclipse/jetty/websocket/server/internal/JettyServerFrameHandlerFactory" -> transformFrameHandlerFactory(
                    ctClass
                )

                "org/eclipse/jetty/websocket/jakarta/common/JakartaWebSocketFrameHandler" -> transformWebSocketFrameHandler(
                    ctClass
                )

                "org/eclipse/jetty/websocket/jakarta/client/internal/JsrUpgradeListener" -> transformJsrUpgradeListener(
                    ctClass
                )
            }
        } catch (e: ClassNotFoundException) {
            logger.error { "transform: Skipping Jetty-11 transformations (probably Jetty versions isn't 11): $e" }
        }
    }

    private fun transformFrameHandler(ctClass: CtClass) = CtNewMethod.abstractMethod(
        ctClass.classPool.get("java.util.Map"),
        "getHandshakeHeaders",
        null,
        null,
        ctClass
    ).also(ctClass::addMethod)

    private fun transformWebSocketFrameHandler(ctClass: CtClass) {
        CtField.make(
            "private java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders = null;",
            ctClass
        ).also(ctClass::addField)
        CtMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.lang.String>*/ getHandshakeHeaders() {
                return this.handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        CtMethod.make(
            """
            public void setHandshakeHeaders(java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders) {
                this.handshakeHeaders = handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        val method = ctClass.getMethod(
            "acceptMessage",
            "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;)V"
        )
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1.isDataFrame() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.handshakeHeaders)) {
                byte[] bytes = new byte[$1.getPayload().limit()];
                int position = $1.getPayload().position();                
                $1.getPayload().get(bytes);
                $1.getPayload().position(position);
                java.lang.Integer drillIndex = ${this::class.java.name}.INSTANCE.${this::retrieveDrillHeadersIndex.name}(bytes);
                if (drillIndex != null) {
                    $1.getPayload().limit(drillIndex.intValue());
                }
            }
            """.trimIndent()
        )
        method.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1.isDataFrame() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.handshakeHeaders)) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformCoreSession(ctClass: CtClass) = ctClass
        .getMethod("sendFrame", "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;Z)V")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.handler.getHandshakeHeaders())) {
                byte[] bytes = new byte[$1.getPayload().limit()];
                $1.getPayload().get(bytes);
                $1.getPayload().clear();
                $1.setPayload(java.nio.ByteBuffer.wrap(${this::class.java.name}.INSTANCE.storeDrillHeaders(bytes)));
            }
            """.trimIndent()
        )

    private fun transformJsrUpgradeListener(ctClass: CtClass) = ctClass
        .getMethod(
            "onHandshakeResponse",
            "(Lorg/eclipse/jetty/client/HttpRequest;Lorg/eclipse/jetty/client/HttpResponse;)V"
        )
        .insertCatching(
            CtBehavior::insertAfter,
            """
            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = $2.getHeaders().getFieldNames().asIterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = (java.lang.String)headerNames.next();
                java.lang.String header = java.lang.String.join(",", $2.getHeaders().getValuesList(headerName));
                allHeaders.put(headerName, header);
            }
            org.eclipse.jetty.websocket.core.FrameHandler frameHandler = ((org.eclipse.jetty.websocket.jakarta.client.internal.JakartaClientUpgradeRequest)$1).getFrameHandler();
            ((org.eclipse.jetty.websocket.jakarta.common.JakartaWebSocketFrameHandler)frameHandler).setHandshakeHeaders(allHeaders);
            """.trimIndent()
        )

    private fun transformFrameHandlerFactory(ctClass: CtClass) = ctClass
        .getMethod(
            "newFrameHandler",
            "(Ljava/lang/Object;Lorg/eclipse/jetty/websocket/core/server/ServerUpgradeRequest;Lorg/eclipse/jetty/websocket/core/server/ServerUpgradeResponse;)Lorg/eclipse/jetty/websocket/core/FrameHandler;"
        )
        .insertCatching(
            CtBehavior::insertAfter,
            """
            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
            java.util.Map/*<java.lang.String, java.util.List<java.lang.String>>*/ upgradeHeaders = $2.getHeadersMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = upgradeHeaders.keySet().iterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = headerNames.next();
                java.lang.String header = java.lang.String.join(",", (java.util.List/*<java.lang.String>*/)upgradeHeaders.get(headerName));
                allHeaders.put(headerName, header);
            }
            ((org.eclipse.jetty.websocket.common.JettyWebSocketFrameHandler)${'$'}_).setHandshakeHeaders(allHeaders);
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
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $1.addHeader(new org.eclipse.jetty.http.HttpField("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true"));
            }
            """.trimIndent()
        )

    private fun transformCreatorNegotiator(ctClass: CtClass) = ctClass
        .getMethod(
            "negotiate",
            "(Lorg/eclipse/jetty/websocket/core/server/WebSocketNegotiation;)Lorg/eclipse/jetty/websocket/core/FrameHandler;"
        )
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $1.getResponse().setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )

}
