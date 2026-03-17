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
package com.epam.drill.agent.instrument.undertow

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.PayloadProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class UndertowWsMessagesTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    PayloadProcessor, AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    private val proxyDelegate = UndertowWsMessagesProxyDelegate(this)

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) = listOf(
        "io/undertow/websockets/jsr/FrameHandler",
        "io/undertow/websockets/jsr/JsrWebSocketFilter",
        "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint",
        "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint\$BasicWebSocketSessionRemoteEndpoint",
        "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint\$AsyncWebSocketSessionRemoteEndpoint",
        "io/undertow/websockets/core/WebSockets",
        "org/springframework/web/reactive/socket/adapter/UndertowWebSocketHandlerAdapter",
        "org/springframework/web/reactive/socket/adapter/UndertowWebSocketSession"
    ).contains(className) || "io/undertow/websockets/client/WebSocketClientHandshake" == superName

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting UndertowWsMessagesTransformer for $className..." }
        when (className) {
            "io/undertow/websockets/jsr/FrameHandler" -> transformFrameHandler(ctClass)
            "io/undertow/websockets/jsr/JsrWebSocketFilter" -> transformWebSocketFilter(ctClass)
            "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint" -> transformRemoteEndpoint(ctClass)
            "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint\$BasicWebSocketSessionRemoteEndpoint" -> transformSessionRemoteEndpoint(
                ctClass
            )

            "io/undertow/websockets/jsr/WebSocketSessionRemoteEndpoint\$AsyncWebSocketSessionRemoteEndpoint" -> transformSessionRemoteEndpoint(
                ctClass
            )

            "io/undertow/websockets/core/WebSockets" -> transformWebSockets(ctClass)
            "org/springframework/web/reactive/socket/adapter/UndertowWebSocketHandlerAdapter" -> transformSpringWebSocketHandlerAdapter(
                ctClass
            )

            "org/springframework/web/reactive/socket/adapter/UndertowWebSocketSession" -> transformSpringWebSocketSession(
                ctClass
            )
        }
        when (ctClass.superclass.name) {
            "io.undertow.websockets.client.WebSocketClientHandshake" -> transformClientHandshake(ctClass)
        }
    }

    private fun transformFrameHandler(ctClass: CtClass) {
        val textMessageProxyClass = proxyDelegate.getTextMessageProxy(ctClass.classPool).name
        val binaryMessageProxyClass = proxyDelegate.getBinaryMessageProxy(ctClass.classPool).name
        val createProxyCode: (String) -> String = { proxy ->
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeHeaders())) {
                $1 = new $proxy($1);
            }
            """.trimIndent()
        }
        val removeHeadersCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeHeaders())) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        ctClass.getMethod(
            "invokeTextHandler",
            "(Lio/undertow/websockets/core/BufferedTextMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V"
        )
            .also { it.insertCatching(CtBehavior::insertBefore, createProxyCode(textMessageProxyClass)) }
            .also { it.insertCatching({ insertAfter(it, true) }, removeHeadersCode) }
        ctClass.getMethod(
            "invokeBinaryHandler",
            "(Lio/undertow/websockets/core/BufferedBinaryMessage;Lio/undertow/websockets/jsr/FrameHandler\$HandlerWrapper;Z)V"
        )
            .also { it.insertCatching(CtBehavior::insertBefore, createProxyCode(binaryMessageProxyClass)) }
            .also { it.insertCatching({ insertAfter(it, true) }, removeHeadersCode) }
    }

    private fun transformSpringWebSocketHandlerAdapter(ctClass: CtClass) {
        val textMessageProxyClass = proxyDelegate.getTextMessageProxy(ctClass.classPool).name
        val binaryMessageProxyClass = proxyDelegate.getBinaryMessageProxy(ctClass.classPool).name
        val createProxyCode: (String) -> String = { proxy ->
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeInfo().getHeaders().toSingleValueMap())) {
                $2 = new $proxy($2);
            }
            """.trimIndent()
        }
        val removeHeadersCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this.session.getHandshakeInfo().getHeaders().toSingleValueMap())) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        ctClass.getMethod(
            "onFullTextMessage",
            "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/BufferedTextMessage;)V"
        )
            .also { it.insertCatching(CtBehavior::insertBefore, createProxyCode(textMessageProxyClass)) }
            .also { it.insertCatching({ insertAfter(it, true) }, removeHeadersCode) }
        ctClass.getMethod(
            "onFullBinaryMessage",
            "(Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/BufferedBinaryMessage;)V"
        )
            .also { it.insertCatching(CtBehavior::insertBefore, createProxyCode(binaryMessageProxyClass)) }
            .also { it.insertCatching({ insertAfter(it, true) }, removeHeadersCode) }
    }

    private fun transformClientHandshake(ctClass: CtClass) {
        ctClass.getMethod("createHeaders", "()Ljava/util/Map;").insertCatching(
            CtBehavior::insertAfter,
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                ${'$'}_.put("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
            }
            """.trimIndent()
        )
    }

    private fun transformWebSocketFilter(ctClass: CtClass) = try {
        ctClass.getMethod(
            "doFilter",
            "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;Ljakarta/servlet/FilterChain;)V"
        )
            .insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                    ((jakarta.servlet.http.HttpServletResponse)$2).setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
                }
                """.trimIndent()
            )
    } catch (e: NotFoundException) {
        ctClass.getMethod(
            "doFilter",
            "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V"
        )
            .insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()) {
                    ((javax.servlet.http.HttpServletResponse)$2).setHeader("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", "true");
                }
                """.trimIndent()
            )
    }

    private fun transformRemoteEndpoint(ctClass: CtClass) {
        CtMethod.make(
            """
            public io.undertow.websockets.jsr.UndertowSession getUndertowSession() {
                return this.undertowSession;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
    }

    private fun transformSessionRemoteEndpoint(ctClass: CtClass) {
        val propagateHandshakeHeaderCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && this$0.getUndertowSession().getHandshakeHeaders() != null) {
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                drillHeaders.put("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", this$0.getUndertowSession().getHandshakeHeaders().get("${PayloadProcessor.HEADER_WS_PER_MESSAGE}"));
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
            }
            """.trimIndent()
        ctClass.declaringClass.defrost()
        if (ctClass.simpleName == "WebSocketSessionRemoteEndpoint\$AsyncWebSocketSessionRemoteEndpoint") {
            try {
                ctClass.getMethod("sendText", "(Ljava/lang/String;Ljakarta/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
                ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;Ljakarta/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
                ctClass.getMethod("sendObject", "(Ljava/lang/Object;Ljakarta/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            } catch (e: NotFoundException) {
                ctClass.getMethod("sendText", "(Ljava/lang/String;Ljavax/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
                ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;Ljavax/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
                ctClass.getMethod("sendObject", "(Ljava/lang/Object;Ljavax/websocket/SendHandler;)V")
                    .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            }
            ctClass.getMethod("sendText", "(Ljava/lang/String;)Ljava/util/concurrent/Future;")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;)Ljava/util/concurrent/Future;")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            ctClass.getMethod("sendObject", "(Ljava/lang/Object;)Ljava/util/concurrent/Future;")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
        }
        if (ctClass.simpleName == "WebSocketSessionRemoteEndpoint\$BasicWebSocketSessionRemoteEndpoint") {
            ctClass.getMethod("sendText", "(Ljava/lang/String;)V")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;)V")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            ctClass.getMethod("sendObject", "(Ljava/lang/Object;)V")
                .insertCatching(CtBehavior::insertBefore, propagateHandshakeHeaderCode)
            ctClass.getMethod("sendText", "(Ljava/lang/String;Z)V").insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                        && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                        && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this$0.getUndertowSession().getHandshakeHeaders())) {
                    $1 = ${this::class.java.name}.INSTANCE.storeDrillHeaders($1);
                }
                """.trimIndent()
            )
            ctClass.getMethod("sendBinary", "(Ljava/nio/ByteBuffer;Z)V").insertCatching(
                CtBehavior::insertBefore,
                """
                if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                        && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                        && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(this$0.getUndertowSession().getHandshakeHeaders())) {
                    byte[] modified = ${this::class.java.name}.INSTANCE.storeDrillHeaders(org.xnio.Buffers.take($1));
                    $1.clear();
                    $1 = java.nio.ByteBuffer.wrap(modified);
                }
                """.trimIndent()
            )
        }
    }

    private fun transformSpringWebSocketSession(ctClass: CtClass) {
        ctClass.getMethod("sendMessage", "(Lorg/springframework/web/reactive/socket/WebSocketMessage;)Z")
            .insertCatching(
                CtBehavior::insertBefore,
                """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && this.getHandshakeInfo().getHeaders().get("${PayloadProcessor.HEADER_WS_PER_MESSAGE}") != null) {
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                drillHeaders.put("${PayloadProcessor.HEADER_WS_PER_MESSAGE}", this.getHandshakeInfo().getHeaders().get("${PayloadProcessor.HEADER_WS_PER_MESSAGE}").get(0));
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
            }
            """.trimIndent()
            )
    }

    private fun transformWebSockets(ctClass: CtClass) {
        val wrapByteBufferCode =
            """
            if (${this::class.java.name}.INSTANCE.${this::isPayloadProcessingEnabled.name}()
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()
                    && ${this::class.java.name}.INSTANCE.${this::isPayloadProcessingSupported.name}(${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}())) {
                byte[] modified = ${this::class.java.name}.INSTANCE.storeDrillHeaders(org.xnio.Buffers.take($1));
                $1.clear();
                $1 = java.nio.ByteBuffer.wrap(modified);
            }
            """.trimIndent()
        ctClass.getMethod(
            "sendBlockingInternal",
            "(Ljava/nio/ByteBuffer;Lio/undertow/websockets/core/WebSocketFrameType;Lio/undertow/websockets/core/WebSocketChannel;)V"
        )
            .insertCatching(CtBehavior::insertBefore, wrapByteBufferCode)
        ctClass.getMethod(
            "sendInternal",
            "(Ljava/nio/ByteBuffer;Lio/undertow/websockets/core/WebSocketFrameType;Lio/undertow/websockets/core/WebSocketChannel;Lio/undertow/websockets/core/WebSocketCallback;Ljava/lang/Object;J)V"
        )
            .insertCatching(CtBehavior::insertBefore, wrapByteBufferCode)
    }

}
