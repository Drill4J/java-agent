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
import com.epam.drill.agent.instrument.request.HttpRequest
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.request.RequestProcessor
import com.epam.drill.agent.instrument.util.createAndTransform

actual object TomcatTransformer : AbstractTransformer() {

    private val logger = KotlinLogging.logger {}

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        return try {
            val adminHeader = HeadersRetriever.adminAddressHeader()
            val adminUrl = HeadersRetriever.adminAddressValue()
            val agentIdHeader = HeadersRetriever.agentIdHeader()
            val agentIdValue = HeadersRetriever.agentIdHeaderValue()
            logger.info { "starting TomcatTransformer with admin host $adminUrl..." }
            ctClass.run {
                val method = getMethod(
                    "doFilter",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"
                ) ?: run {
                    return null
                }
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
                                if (headerName.startsWith("${HttpRequest.DRILL_HEADER_PREFIX}") && tomcatResponse.getHeader(headerName) == null) {
                                    tomcatResponse.addHeader(headerName, header);
                                }
                            }
                            ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                        }
                    """.trimIndent()
                )
                method.insertCatching(
                    CtMethod::insertAfter,
                    """
                       ${RequestProcessor::class.java.name}.INSTANCE.${RequestProcessor::processServerResponse.name}();
                    """.trimIndent()
                )
                return toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }

    override fun logError(exception: Throwable, message: String) = logger.error(exception) { message }

}
