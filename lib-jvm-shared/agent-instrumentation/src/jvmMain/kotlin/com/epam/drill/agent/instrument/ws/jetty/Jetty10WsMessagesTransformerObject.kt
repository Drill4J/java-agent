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
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class Jetty10WsMessagesTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    PayloadProcessor, AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) = listOf(
        "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient",
        "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler",
        "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketRemoteEndpoint",
        "org/eclipse/jetty/websocket/javax/client/internal/JsrUpgradeListener",
        "org/eclipse/jetty/websocket/javax/server/internal/JavaxWebSocketCreator",
        "org/eclipse/jetty/websocket/javax/server/internal/JavaxWebSocketServerFrameHandlerFactory"
    ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting Jetty10WsMessagesTransformerObject for $className..." }
        try {
            if (className != "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler")
                ctClass.classPool.classLoader.loadClass("org.eclipse.jetty.websocket.javax.common.JavaxWebSocketFrameHandler")
            when (className) {
                "org/eclipse/jetty/websocket/core/client/WebSocketCoreClient" -> transformWebSocketCoreClient(ctClass)
                "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler" -> transformJavaxWebSocketFrameHandler(
                    ctClass
                )

                "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketRemoteEndpoint" -> transformWebSocketRemoteEndpoint(
                    ctClass
                )

                "org/eclipse/jetty/websocket/javax/client/internal/JsrUpgradeListener" -> transformJsrUpgradeListener(
                    ctClass
                )

                "org/eclipse/jetty/websocket/javax/server/internal/JavaxWebSocketCreator" -> transformWebSocketCreator(
                    ctClass
                )

                "org/eclipse/jetty/websocket/javax/server/internal/JavaxWebSocketServerFrameHandlerFactory" -> transformFrameHandlerFactory(
                    ctClass
                )
            }
        } catch (e: ClassNotFoundException) {
            logger.error { "transform: Skipping Jetty-10 transformations (probably Jetty versions isn't 10): $e" }
        }
    }

    private fun transformJavaxWebSocketFrameHandler(ctClass: CtClass) {
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
        val acceptMessageMethod = ctClass.getMethod(
            "acceptMessage",
            "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;)V"
        )
        acceptMessageMethod.insertCatching(
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
        acceptMessageMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1.isDataFrame() && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.handshakeHeaders)) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformWebSocketRemoteEndpoint(ctClass: CtClass) = ctClass
        .getMethod("sendFrame", "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;Z)V")
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(((org.eclipse.jetty.websocket.javax.common.JavaxWebSocketFrameHandler)this.session.getFrameHandler()).getHandshakeHeaders())) {
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
            org.eclipse.jetty.websocket.core.FrameHandler frameHandler = ((org.eclipse.jetty.websocket.javax.client.internal.JavaxClientUpgradeRequest)$1).getFrameHandler();
            ((org.eclipse.jetty.websocket.javax.common.JavaxWebSocketFrameHandler)frameHandler).setHandshakeHeaders(allHeaders);
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
            ((org.eclipse.jetty.websocket.javax.common.JavaxWebSocketFrameHandler)${'$'}_).setHandshakeHeaders(allHeaders);
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

    private fun transformWebSocketCreator(ctClass: CtClass) = ctClass
        .getMethod(
            "createWebSocket",
            "(Lorg/eclipse/jetty/websocket/core/server/ServerUpgradeRequest;Lorg/eclipse/jetty/websocket/core/server/ServerUpgradeResponse;)Ljava/lang/Object;"
        )
        .insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $2.setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )

}
