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
package com.epam.drill.agent.instrument.netty

import com.epam.drill.agent.common.configuration.AgentConfiguration
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.common.request.HeadersRetriever
import com.epam.drill.agent.instrument.AbstractPropagationTransformer
import com.epam.drill.agent.instrument.COMPILER_GENERATED_NAMES_PREFIX
import com.epam.drill.agent.instrument.NETTY_CHANNEL_HANDLER_CONTEXT
import java.security.ProtectionDomain

/**
 * Transformer for simple Netty-based web servers
 *
 * Tested with:
 *     io.netty:netty-codec-http:4.1.106.Final
 */
abstract class NettyHttpServerTransformerObject(
    private val headersRetriever: HeadersRetriever,
    agentConfiguration: AgentConfiguration
) : HeadersProcessor, AbstractPropagationTransformer(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(
        className: String,
        superName: String?,
        interfaces: Array<String?>
    ): Boolean = COMPILER_GENERATED_NAMES_PREFIX !in className && className.startsWith(NETTY_CHANNEL_HANDLER_CONTEXT)

    override fun transform(
        className: String,
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ) {
        if (pool.find(HTTP_REQUEST) == null) {
            logger.debug { "transform: Skipping $className because $HTTP_REQUEST class is not available" }
            return
        }
        if (pool.find(HTTP_RESPONSE) == null) {
            logger.debug { "transform: Skipping $className because $HTTP_RESPONSE class is not available" }
            return
        }
        val invokeChannelReadMethod = ctClass.getMethod("fireChannelRead", "(Ljava/lang/Object;)Lio/netty/channel/ChannelHandlerContext;")
        invokeChannelReadMethod.insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1 instanceof $HTTP_REQUEST) {
                $HTTP_REQUEST nettyRequest = ($HTTP_REQUEST) $1;
                io.netty.handler.codec.http.HttpHeaders headers = nettyRequest.headers();
                java.util.Iterator iterator = headers.names().iterator();
                java.util.Map allHeaders = new java.util.HashMap();
                while(iterator.hasNext()){
                    java.lang.String headerName = (String) iterator.next();
                    java.lang.String headerValue = headers.get(headerName);
                    allHeaders.put(headerName, headerValue);
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                if (drillHeaders != null) {
                    io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("$DRILL_HTTP_CONTEXT_KEY");
                    this.channel().attr(drillContextKey).set(drillHeaders);
                }
            }
            """.trimIndent()
        )
        invokeChannelReadMethod.insertCatching(
            { insertAfter(it, true) },
            """
            if ($1 instanceof $HTTP_REQUEST) {
                ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            }
            """.trimIndent()
        )

        val adminHeader = headersRetriever.adminAddressHeader()
        val adminUrl = headersRetriever.adminAddressValue()
        val agentIdHeader = headersRetriever.agentIdHeader()
        val agentIdValue = headersRetriever.agentIdHeaderValue()
        ctClass.getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V").insertCatching(
            CtBehavior::insertBefore,
            """
            if ($1 instanceof $HTTP_RESPONSE) {
                io.netty.util.AttributeKey drillContextKey = io.netty.util.AttributeKey.valueOf("$DRILL_HTTP_CONTEXT_KEY");                                            
                io.netty.util.Attribute drillContextAttr = this.channel().attr(drillContextKey);
                java.util.Map drillHeaders = (java.util.Map) drillContextAttr.get();
                drillContextAttr.compareAndSet(drillHeaders, null);
                $HTTP_RESPONSE nettyResponse = ($HTTP_RESPONSE) $1;
                if (!"$adminUrl".equals(nettyResponse.headers().get("$adminHeader"))) {
                    nettyResponse.headers().add("$adminHeader", "$adminUrl");
                    nettyResponse.headers().add("$agentIdHeader", "$agentIdValue");
                }                
                if (drillHeaders != null) {
                    java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                    while (iterator.hasNext()) {
                         java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                         String headerName = (String) entry.getKey();
                         String headerValue = (String) entry.getValue();
                         if (!nettyResponse.headers().contains(headerName)) {
                             nettyResponse.headers().add(headerName, headerValue);
                         }
                    }                    
                }
            }
            """.trimIndent()
        )
    }

}
