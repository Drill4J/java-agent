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

import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.request.HttpRequest
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.request.RequestProcessor

actual object NettyTransformer : TransformerObject, AbstractTransformerObject() {

    private const val DEFAULT_HTTP_REQUEST = "io.netty.handler.codec.http.DefaultHttpRequest"
    private const val DEFAULT_HTTP_RESPONSE = "io.netty.handler.codec.http.DefaultHttpResponse"

    override val logger = KotlinLogging.logger {}

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("invokeChannelRead", "(Ljava/lang/Object;)V").insertCatching(
            CtMethod::insertBefore,
            """
                if ($1 instanceof $DEFAULT_HTTP_REQUEST) {
                    $DEFAULT_HTTP_REQUEST nettyRequest = ($DEFAULT_HTTP_REQUEST) $1;
                    io.netty.handler.codec.http.HttpHeaders headers = nettyRequest.headers();
                    java.util.Iterator iterator = headers.names().iterator();
                    java.util.Map allHeaders = new java.util.HashMap();
                    while(iterator.hasNext()){
                        java.lang.String headerName = (String) iterator.next();
                        java.lang.String headerValue = headers.get(headerName);
                        allHeaders.put(headerName, headerValue);
                    }
                    ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                }
            """.trimIndent()
        )
        val adminHeader = HeadersRetriever.adminAddressHeader()
        val adminUrl = HeadersRetriever.adminAddressValue()
        val agentIdHeader = HeadersRetriever.agentIdHeader()
        val agentIdValue = HeadersRetriever.agentIdHeaderValue()
        val writeMethod = ctClass.getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V")
        writeMethod.insertCatching(
            CtMethod::insertBefore,
            """
                if ($1 instanceof $DEFAULT_HTTP_RESPONSE) {
                    $DEFAULT_HTTP_RESPONSE nettyResponse = ($DEFAULT_HTTP_RESPONSE) $1;
                    if (!"$adminUrl".equals(nettyResponse.headers().get("$adminHeader"))) {
                        nettyResponse.headers().add("$adminHeader", "$adminUrl");
                        nettyResponse.headers().add("$agentIdHeader", "$agentIdValue");
                    }
                }
                if ($1 instanceof $DEFAULT_HTTP_REQUEST) {
                    $DEFAULT_HTTP_REQUEST nettyRequest = ($DEFAULT_HTTP_REQUEST) $1;
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
            """.trimIndent()
        )
        writeMethod.insertCatching(
            CtMethod::insertAfter,
            """
                if ($1 instanceof $DEFAULT_HTTP_RESPONSE) {
                    ${RequestProcessor::class.java.name}.INSTANCE.${RequestProcessor::processServerResponse.name}();
                }
            """.trimIndent()
        )
    }

}
