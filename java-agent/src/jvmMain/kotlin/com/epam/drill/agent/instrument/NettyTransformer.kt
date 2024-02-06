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
package com.epam.drill.agent.instrument

import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.error.wrapCatching
import com.epam.drill.agent.instrument.request.HttpRequest
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.instrument.util.createAndTransform

actual object NettyTransformer {

    private val logger = KotlinLogging.logger {}

    private val DefaultHttpRequest = "io.netty.handler.codec.http.DefaultHttpRequest"
    private val DefaultHttpResponse = "io.netty.handler.codec.http.DefaultHttpResponse"

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        return try {
            ctClass.run {
                val methodChannelRead = getMethod("invokeChannelRead", "(Ljava/lang/Object;)V")
                methodChannelRead.wrapCatching(
                    CtMethod::insertBefore,
                    """
                        if ($1 instanceof $DefaultHttpRequest) {
                            $DefaultHttpRequest nettyRequest = ($DefaultHttpRequest) $1;
                            io.netty.handler.codec.http.HttpHeaders headers = nettyRequest.headers();
                            java.util.Iterator iterator = headers.names().iterator();
                            java.util.Map allHeaders = new java.util.HashMap();
                            while(iterator.hasNext()){
                                java.lang.String headerName = (String) iterator.next();
                                java.lang.String headerValue = headers.get(headerName);
                                allHeaders.put(headerName, headerValue);
                            }         
                            com.epam.drill.agent.request.DrillRequest drillRequest = ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                            if (drillRequest != null) {
                                com.epam.drill.agent.instrument.error.InstrumentationErrorLogger.INSTANCE.info("channelRead propagated: " + drillRequest);
                                io.netty.util.AttributeKey drillContext = io.netty.util.AttributeKey.valueOf("com.epam.drill.agent.request.DrillRequest");
                                this.channel().attr(drillContext).set(drillRequest);                                   
                            }                                                                                       
                        }
                    """.trimIndent()
                )
                methodChannelRead.insertAfter(
                    """    
                        if ($1 instanceof $DefaultHttpRequest) {
                            com.epam.drill.agent.request.RequestHolder.INSTANCE.remove();
                        }
                    """.trimIndent(), true
                )

                val drillAdminHeader = HeadersRetriever.adminAddressHeader()
                val adminUrl = HeadersRetriever.retrieveAdminAddress()
                val writeMethod = getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V")
                writeMethod.wrapCatching(
                    CtMethod::insertBefore,
                    """
                        if ($1 instanceof $DefaultHttpResponse) {
                            $DefaultHttpResponse nettyResponse = ($DefaultHttpResponse) $1;
                            if (!"$adminUrl".equals(nettyResponse.headers().get("$drillAdminHeader"))) {
                                nettyResponse.headers().add("$drillAdminHeader", "$adminUrl");
                                nettyResponse.headers().add("${HeadersRetriever.idHeaderConfigKey()}", "${HeadersRetriever.idHeaderConfigValue()}");
                            }
                        }
                        if ($1 instanceof $DefaultHttpRequest) {
                            $DefaultHttpRequest nettyRequest = ($DefaultHttpRequest) $1;
                            java.util.Map drillHeaders = ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::loadDrillHeaders.name}();
                            if (drillHeaders != null) {
                                java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                                while (iterator.hasNext()) {
                                     java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                                     String headerName = (String) entry.getKey();
                                     String headerValue = (String) entry.getValue();
                                     if (!nettyRequest.headers().contains(headerName)) {
                                        nettyRequest.headers().add(headerName, headerValue);
                                     }
                                }
                            }
                        }  
                        if ($1 instanceof $DefaultHttpResponse) {                            
                            io.netty.util.AttributeKey drillContext = io.netty.util.AttributeKey.valueOf("com.epam.drill.agent.request.DrillRequest");                            
                            io.netty.util.Attribute drillAttr = this.channel().attr(drillContext);
                            com.epam.drill.agent.request.DrillRequest drillRequest = (com.epam.drill.agent.request.DrillRequest) drillAttr.get();
                            drillAttr.compareAndSet(drillRequest, null);
                            if (drillRequest != null) {
                                com.epam.drill.agent.instrument.error.InstrumentationErrorLogger.INSTANCE.info("channelWrite: " + drillRequest + " restored");
                                com.epam.drill.agent.request.RequestHolder.INSTANCE.storeRequest(drillRequest);   
                            }                            
                        }
                    """.trimIndent()
                )
                writeMethod.insertAfter(
                    """     
                        if ($1 instanceof $DefaultHttpRequest) {
                            com.epam.drill.agent.request.RequestHolder.INSTANCE.remove();
                        }                                                
                    """.trimIndent(), true
                )
                toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error. Reason:" }
            null
        }
    }
}
