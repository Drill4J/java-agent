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
import javassist.CtNewMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.ws.AbstractWsTransformerObject

/**
 * Transformer for Jetty based websockets
 *
 * Tested with:
 *     org.eclipse.jetty.websocket:javax-websocket-server-impl:9.4.26.v20200117
 */
abstract class JettyWsServerTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractWsTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        listOf(
            "org/eclipse/jetty/websocket/common/events/AbstractEventDriver",
            "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler",
            "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler",
            "org/eclipse/jetty/websocket/javax/common/UpgradeRequest",
            "org/eclipse/jetty/websocket/javax/client/internal/JavaxClientUpgradeRequest",
            "org/eclipse/jetty/websocket/javax/server/internal/JavaxServerUpgradeRequest"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting JettyWsServerTransformerObject for $className..." }
        when (className) {
            "org/eclipse/jetty/websocket/common/events/AbstractEventDriver" -> transformAbstractEventDriver(ctClass)
            "org/eclipse/jetty/websocket/common/JettyWebSocketFrameHandler" -> transformJettyWebSocketFrameHandler(
                ctClass
            )

            "org/eclipse/jetty/websocket/javax/common/JavaxWebSocketFrameHandler" -> transformJavaxWebSocketFrameHandler(
                ctClass
            )

            "org/eclipse/jetty/websocket/javax/common/UpgradeRequest" -> transformUpgradeRequest(ctClass)
            "org/eclipse/jetty/websocket/javax/client/internal/JavaxClientUpgradeRequest" -> transformJavaxClientUpgradeRequest(
                ctClass
            )

            "org/eclipse/jetty/websocket/javax/server/internal/JavaxServerUpgradeRequest" -> transformJavaxServerUpgradeRequest(
                ctClass
            )
        }
    }

    private fun transformAbstractEventDriver(ctClass: CtClass) {
        val method = ctClass.getMethod("incomingFrame", "(Lorg/eclipse/jetty/websocket/api/extensions/Frame;)V")
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if (($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY)
                    && this.session.getUpgradeRequest() instanceof org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = this.session.getUpgradeRequest().getHeaders().keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = this.session.getUpgradeRequest().getHeaders().get(headerName);
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
            if (($1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.TEXT || $1.getOpCode() == org.eclipse.jetty.websocket.common.OpCode.BINARY)
                    && this.session.getUpgradeRequest() instanceof org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
                    && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformJettyWebSocketFrameHandler(ctClass: CtClass) {
        val method = ctClass.getMethod(
            "acceptMessage",
            "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;)V"
        )
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1.isDataFrame()) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Map/*<java.lang.String, java.util.List<java.lang.String>>*/ upgradeHeaders = this.upgradeRequest.getHeaders();
                java.util.Iterator/*<java.lang.String>*/ headerNames = upgradeHeaders.keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = upgradeHeaders.get(headerName);
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
            if ($1.isDataFrame() && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformJavaxWebSocketFrameHandler(ctClass: CtClass) {
        val method = ctClass.getMethod(
            "acceptMessage",
            "(Lorg/eclipse/jetty/websocket/core/Frame;Lorg/eclipse/jetty/util/Callback;)V"
        )
        method.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1.isDataFrame() && this.upgradeRequest.getHeadersMap() != null ) {
                java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                java.util.Map/*<java.lang.String, java.util.List<java.lang.String>>*/ upgradeHeaders = this.upgradeRequest.getHeadersMap();
                java.util.Iterator/*<java.lang.String>*/ headerNames = upgradeHeaders.keySet().iterator();
                while (headerNames.hasNext()) {
                    java.lang.String headerName = headerNames.next();
                    java.util.List/*<java.lang.String>*/ headerValues = upgradeHeaders.get(headerName);
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
            if ($1.isDataFrame() && this.upgradeRequest.getHeadersMap() != null && ${this::class.java.name}.INSTANCE.${this::hasHeaders.name}()) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )
    }

    private fun transformUpgradeRequest(ctClass: CtClass) {
        CtNewMethod.abstractMethod(
            ctClass.classPool.get("java.util.Map"),
            "getHeadersMap",
            null,
            null,
            ctClass
        ).also(ctClass::addMethod)
    }

    private fun transformJavaxClientUpgradeRequest(ctClass: CtClass) {
        CtNewMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.util.List<java.lang.String>>*/ getHeadersMap() {
                return null;
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
    }

    private fun transformJavaxServerUpgradeRequest(ctClass: CtClass) {
        CtNewMethod.make(
            """
            public java.util.Map/*<java.lang.String, java.util.List<java.lang.String>>*/ getHeadersMap() {
                return this.servletRequest.getHeadersMap();
            }
            """.trimIndent(),
            ctClass
        ).also(ctClass::addMethod)
    }

}
