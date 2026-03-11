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
import javassist.CtField
import javassist.CtMethod
import javassist.NotFoundException
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class TomcatWsMessagesTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor, PayloadProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}
    private var openingSessionHeaders: ThreadLocal<Map<String, String>?> = ThreadLocal()

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) = listOf(
        "org/apache/tomcat/websocket/WsSession",
        "org/apache/tomcat/websocket/WsFrameBase",
        "org/apache/tomcat/websocket/WsRemoteEndpointImplBase",
        "org/apache/tomcat/websocket/WsWebSocketContainer",
        "org/apache/tomcat/websocket/server/WsHttpUpgradeHandler",
        "org/apache/tomcat/websocket/server/UpgradeUtil"
    ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting TomcatWsMessagesTransformer for $className..." }
        if (className != "org/apache/tomcat/websocket/WsSession")
            ctClass.classPool.classLoader.loadClass("org.apache.tomcat.websocket.WsSession")
        when (className) {
            "org/apache/tomcat/websocket/WsSession" -> transformWsSession(ctClass)
            "org/apache/tomcat/websocket/WsFrameBase" -> transformWsFrameBase(ctClass)
            "org/apache/tomcat/websocket/WsRemoteEndpointImplBase" -> transformRemoteEndpointImplBase(ctClass)
            "org/apache/tomcat/websocket/WsWebSocketContainer" -> transformWebSocketContainer(ctClass)
            "org/apache/tomcat/websocket/server/WsHttpUpgradeHandler" -> transformUpgradeHandler(ctClass)
            "org/apache/tomcat/websocket/server/UpgradeUtil" -> transformUpgradeUtil(ctClass)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHandshakeHeaders(headers: Map<String, String>?) = openingSessionHeaders.set(headers)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getHandshakeHeaders() = openingSessionHeaders.get()

    private fun transformWsSession(ctClass: CtClass) {
        CtField.make(
            "private java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders = null;",
            ctClass
        ).also(ctClass::addField)
        CtMethod.make(
            """
            public void setHandshakeHeaders(java.util.Map/*<java.lang.String, java.lang.String>*/ handshakeHeaders) {
                this.handshakeHeaders = handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        CtMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.lang.String>*/ getHandshakeHeaders() {
                return this.handshakeHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
    }

    private fun transformUpgradeHandler(ctClass: CtClass) {
        val method = try {
            ctClass.getMethod("init", "(Ljakarta/servlet/http/WebConnection;)V")
        } catch (e: NotFoundException) {
            ctClass.getMethod("init", "(Ljavax/servlet/http/WebConnection;)V")
        }
        method.insertCatching(
            CtBehavior::insertAfter,
            """
            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = this.handshakeRequest.getHeaders().keySet().iterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = headerNames.next();
                java.util.List/*<java.lang.String>*/ headerValues = this.handshakeRequest.getHeaders().get(headerName);
                java.lang.String header = java.lang.String.join(",", headerValues);
                allHeaders.put(headerName, header);
            }
            this.wsSession.setHandshakeHeaders(allHeaders);
            """.trimIndent()
        )
    }

    private fun transformWsFrameBase(ctClass: CtClass) {
        val sendTextMethod = ctClass.getMethod("sendMessageText", "(Z)V")
        val sendBinaryMethod = ctClass.getMethod("sendMessageBinary", "(Ljava/nio/ByteBuffer;Z)V")
        CtField.make(
            "private java.nio.CharBuffer messageBufferTextTmp = null;",
            ctClass
        ).also(ctClass::addField)
        sendTextMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                java.lang.String retrieved = ${this::class.java.name}.INSTANCE.retrieveDrillHeaders(this.messageBufferText.toString());
                this.messageBufferTextTmp = messageBufferText;
                this.messageBufferText = java.nio.CharBuffer.wrap(retrieved);
            }
            """.trimIndent()
        )
        sendBinaryMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                byte[] bytes = new byte[$1.limit()];
                $1.get(bytes);
                $1 = java.nio.ByteBuffer.wrap(${this::class.java.name}.INSTANCE.retrieveDrillHeaders(bytes));
            }
            """.trimIndent()
        )
        sendTextMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                this.messageBufferText = messageBufferTextTmp;
                this.messageBufferText.clear();
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
        sendBinaryMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformRemoteEndpointImplBase(ctClass: CtClass) {
        val wrapStringCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                $1 = ${this::class.java.name}.INSTANCE.storeDrillHeaders($1);
            }
            """.trimIndent()
        val wrapBinaryCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.wsSession.getHandshakeHeaders())) {
                byte[] bytes = new byte[$1.limit()];
                $1.get(bytes);
                $1.clear();
                $1 = java.nio.ByteBuffer.wrap(${this::class.java.name}.INSTANCE.storeDrillHeaders(bytes));
            }
            """.trimIndent()
        ctClass.getMethod("sendString", "(Ljava/lang/String;)V")
            .insertCatching(CtBehavior::insertBefore, wrapStringCode)
        ctClass.getMethod("sendPartialString", "(Ljava/lang/String;Z)V")
            .insertCatching(CtBehavior::insertBefore, wrapStringCode)
        ctClass.getMethod("sendBytes", "(Ljava/nio/ByteBuffer;)V")
            .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        ctClass.getMethod("sendPartialBytes", "(Ljava/nio/ByteBuffer;Z)V")
            .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        try {
            ctClass.getMethod("sendStringByCompletion", "(Ljava/lang/String;Ljakarta/websocket/SendHandler;)V")
                .insertCatching(CtBehavior::insertBefore, wrapStringCode)
            ctClass.getMethod("sendBytesByCompletion", "(Ljava/nio/ByteBuffer;Ljakarta/websocket/SendHandler;)V")
                .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        } catch (e: NotFoundException) {
            ctClass.getMethod("sendStringByCompletion", "(Ljava/lang/String;Ljavax/websocket/SendHandler;)V")
                .insertCatching(CtBehavior::insertBefore, wrapStringCode)
            ctClass.getMethod("sendBytesByCompletion", "(Ljava/nio/ByteBuffer;Ljavax/websocket/SendHandler;)V")
                .insertCatching(CtBehavior::insertBefore, wrapBinaryCode)
        }
    }

    private fun transformWebSocketContainer(ctClass: CtClass) {
        val createRequestHeadersMethodSignatures = sequenceOf(
            "(Ljava/lang/String;IZLjakarta/websocket/ClientEndpointConfig;)Ljava/util/Map;",
            "(Ljava/lang/String;IZLjavax/websocket/ClientEndpointConfig;)Ljava/util/Map;",
            "(Ljava/lang/String;ILjavax/websocket/ClientEndpointConfig;)Ljava/util/Map;"
        )
        val connectToServerRecursiveMethodSignatures = sequenceOf(
            "(Lorg/apache/tomcat/websocket/ClientEndpointHolder;Ljakarta/websocket/ClientEndpointConfig;Ljava/net/URI;Ljava/util/Set;)Ljakarta/websocket/Session;",
            "(Lorg/apache/tomcat/websocket/ClientEndpointHolder;Ljavax/websocket/ClientEndpointConfig;Ljava/net/URI;Ljava/util/Set;)Ljavax/websocket/Session;",
            "(Ljavax/websocket/Endpoint;Ljavax/websocket/ClientEndpointConfig;Ljava/net/URI;Ljava/util/Set;)Ljavax/websocket/Session;"
        )
        val getCreateRequestHeadersMethod: (String) -> CtMethod? = {
            ctClass
                .runCatching { this.getMethod("createRequestHeaders", it) }
                .getOrNull()
        }
        val getConnectToServerRecursiveMethod: (String) -> CtMethod? = {
            ctClass
                .runCatching { this.getMethod("connectToServerRecursive", it) }
                .getOrNull()
        }
        createRequestHeadersMethodSignatures.mapNotNull(getCreateRequestHeadersMethod).first().insertCatching(
            CtBehavior::insertAfter,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                ${'$'}_.put("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", java.util.Collections.singletonList("true"));
            }
            """.trimIndent()
        )
        connectToServerRecursiveMethodSignatures.mapNotNull(getConnectToServerRecursiveMethod).first().insertCatching(
            CtBehavior::insertAfter,
            """
            ((org.apache.tomcat.websocket.WsSession)${'$'}_).setHandshakeHeaders(${this::class.java.name}.INSTANCE.${this::getHandshakeHeaders.name}());
            ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(null);
            """.trimIndent()
        )
        ctClass.getMethod(
            "processResponse",
            "(Ljava/nio/ByteBuffer;Lorg/apache/tomcat/websocket/AsyncChannelWrapper;J)Lorg/apache/tomcat/websocket/WsWebSocketContainer\$HttpResponse;"
        )
            .insertCatching(
                CtBehavior::insertAfter,
                """
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = ${'$'}_.getHandshakeResponse().getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = ${'$'}_.getHandshakeResponse().getHeaders().get(headerName);
                    java.lang.String header = java.lang.String.join(",", headerValues);
                    allHeaders.put(headerName, header);
                }
                ${this::class.java.name}.INSTANCE.${this::setHandshakeHeaders.name}(allHeaders);
                """.trimIndent()
            )
    }

    private fun transformUpgradeUtil(ctClass: CtClass) {
        val signatures = sequenceOf(
            "(Lorg/apache/tomcat/websocket/server/WsServerContainer;Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;Ljakarta/websocket/server/ServerEndpointConfig;Ljava/util/Map;)V",
            "(Lorg/apache/tomcat/websocket/server/WsServerContainer;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljavax/websocket/server/ServerEndpointConfig;Ljava/util/Map;)V"
        )
        val getMethod: (String) -> CtMethod? = {
            ctClass
                .runCatching { this.getMethod("doUpgrade", it) }
                .getOrNull()
        }
        signatures.mapNotNull(getMethod).first().insertCatching(
            CtBehavior::insertAfter,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                $3.setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )
    }

}
