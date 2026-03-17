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
import javassist.CtField
import javassist.CtMethod
import javassist.NotFoundException
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

abstract class UndertowWsClientTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) = listOf(
        "io/undertow/websockets/jsr/UndertowSession",
        "io/undertow/websockets/jsr/ServerWebSocketContainer\$ClientNegotiation"
    ).contains(className) || "io/undertow/websockets/client/WebSocketClientHandshake" == superName

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting UndertowWsClientTransformer for $className..." }
        when (className) {
            "io/undertow/websockets/jsr/UndertowSession" -> transformSession(ctClass)
            "io/undertow/websockets/jsr/ServerWebSocketContainer\$ClientNegotiation" -> transformClientNegotiation(
                ctClass
            )
        }
        when (ctClass.superclass.name) {
            "io.undertow.websockets.client.WebSocketClientHandshake" -> transformClientHandshake(ctClass)
        }
    }

    private fun transformSession(ctClass: CtClass) {
        try {
            ctClass.getField("handshakeHeaders")
        } catch (e: NotFoundException) {
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
        }
        ctClass.constructors[0].insertCatching(
            CtBehavior::insertAfter,
            """
            if (this.clientConnectionBuilder != null) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ responseHeaders =
                        ((io.undertow.websockets.jsr.ServerWebSocketContainer.ClientNegotiation) this.clientConnectionBuilder.getClientNegotiation()).getResponseHeaders();
                if (this.handshakeHeaders == null && responseHeaders != null) {
                    this.handshakeHeaders = responseHeaders;
                }
            }
            """.trimIndent()
        )
    }

    private fun transformClientHandshake(ctClass: CtClass) {
        ctClass.getMethod("createHeaders", "()Ljava/util/Map;").insertCatching(
            CtBehavior::insertAfter,
            """
            if (${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) { 
                java.util.Map headers = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                java.util.Iterator iterator = headers.entrySet().iterator();             
                while (iterator.hasNext()) {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                    ${'$'}_.put((String) entry.getKey(), (String) entry.getValue());
                }
                ${this::class.java.name}.INSTANCE.${this::logInjectingHeaders.name}(headers);
            }
            """.trimIndent()
        )
    }

    private fun transformClientNegotiation(ctClass: CtClass) {
        CtField.make(
            "private java.util.Map/*<java.lang.String, java.lang.String>*/ responseHeaders = null;",
            ctClass
        ).also(ctClass::addField)
        CtMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.lang.String>*/ getResponseHeaders() {
                return this.responseHeaders;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
        ctClass.getMethod("afterRequest", "(Ljava/util/Map;)V").insertCatching(
            CtBehavior::insertAfter,
            """
            this.responseHeaders = new java.util.HashMap();
            java.util.Iterator/*<java.lang.String>*/ headerNames = $1.keySet().iterator();
            while (headerNames.hasNext()) {
                java.lang.String headerName = headerNames.next();
                java.util.List/*<java.lang.String>*/ headerValues = $1.get(headerName);
                java.lang.String header = java.lang.String.join(",", headerValues);
                this.responseHeaders.put(headerName, header);
            }
            """.trimIndent()
        )
    }

}
