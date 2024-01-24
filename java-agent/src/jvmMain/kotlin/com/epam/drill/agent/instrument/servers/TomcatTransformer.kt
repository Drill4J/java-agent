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
package com.epam.drill.agent.instrument.servers

import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.DrillRequestHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.request.RequestHolder

actual object TomcatTransformer :
    TransformerObject,
    AbstractTransformerObject(),
    HeadersProcessor by DrillRequestHeadersProcessor(HeadersRetriever, RequestHolder) {

    override val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        throw NotImplementedError()

    override fun transform(className: String, ctClass: CtClass) {
        val adminHeader = HeadersRetriever.adminAddressHeader()
        val adminUrl = HeadersRetriever.adminAddressValue()
        val agentIdHeader = HeadersRetriever.agentIdHeader()
        val agentIdValue = HeadersRetriever.agentIdHeaderValue()
        logger.info { "transform: Starting TomcatTransformer with admin host $adminUrl..." }
        val method = ctClass.getMethod("doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V")
        method.insertCatching(
            CtMethod::insertBefore,
            """
                if ($1 instanceof org.apache.catalina.connector.RequestFacade && $2 instanceof org.apache.catalina.connector.ResponseFacade) {
                    org.apache.catalina.connector.ResponseFacade tomcatResponse = (org.apache.catalina.connector.ResponseFacade)$2;
                    if (!"$adminUrl".equals(tomcatResponse.getHeader("$adminHeader"))) {
                        tomcatResponse.addHeader("$adminHeader", "$adminUrl");
                        tomcatResponse.addHeader("$agentIdHeader", "$agentIdValue");
                    }
                    
                    org.apache.catalina.connector.RequestFacade tomcatRequest = (org.apache.catalina.connector.RequestFacade)${'$'}1;
                    java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                    java.util.Enumeration/*<String>*/ headerNames = tomcatRequest.getHeaderNames();
                    while (headerNames.hasMoreElements()) {
                        java.lang.String headerName = (java.lang.String) headerNames.nextElement();
                        java.lang.String header = tomcatRequest.getHeader(headerName);
                        allHeaders.put(headerName, header);
                        if (headerName.startsWith("${HeadersProcessor.DRILL_HEADER_PREFIX}") && tomcatResponse.getHeader(headerName) == null) {
                            tomcatResponse.addHeader(headerName, header);
                        }
                    }
                    ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(allHeaders);
                }
            """.trimIndent()
        )
        method.insertCatching(
            CtMethod::insertAfter,
            """
               ${this::class.java.name}.INSTANCE.${this::removeHeaders.name}();
            """.trimIndent()
        )
    }

}
