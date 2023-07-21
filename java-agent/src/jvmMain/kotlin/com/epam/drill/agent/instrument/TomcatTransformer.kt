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

import com.epam.drill.agent.*
import com.epam.drill.agent.instrument.util.*
import com.epam.drill.request.*
import com.epam.drill.request.HttpRequest.DRILL_HEADER_PREFIX
import javassist.*
import mu.KotlinLogging

actual object TomcatTransformer {

    private val logger = KotlinLogging.logger {}

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        return try {
            val adminUrl = HeadersRetriever.retrieveAdminAddress()
            logger.info { "starting TomcatTransformer with admin host $adminUrl..." }
            ctClass.run {
                val drillAdminHeader = HeadersRetriever.adminAddressHeader()
                val method = getMethod(
                    "doFilter",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"
                ) ?: run {
                    return null
                }
                method.wrapCatching(
                    CtMethod::insertBefore,
                    """
                        if ($1 instanceof org.apache.catalina.connector.RequestFacade && $2 instanceof org.apache.catalina.connector.ResponseFacade) {
                            org.apache.catalina.connector.ResponseFacade tomcatResponse = (org.apache.catalina.connector.ResponseFacade)$2;
                            if (!"$adminUrl".equals(tomcatResponse.getHeader("$drillAdminHeader"))) {
                                tomcatResponse.addHeader("$drillAdminHeader", "$adminUrl");
                                tomcatResponse.addHeader("${HeadersRetriever.idHeaderConfigKey()}", "${HeadersRetriever.idHeaderConfigValue()}");
                            }
                            
                            org.apache.catalina.connector.RequestFacade tomcatRequest = (org.apache.catalina.connector.RequestFacade)${'$'}1;
                            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                            java.util.Enumeration/*<String>*/ headerNames = tomcatRequest.getHeaderNames();
                            while (headerNames.hasMoreElements()) {
                                java.lang.String headerName = (java.lang.String) headerNames.nextElement();
                                java.lang.String header = tomcatRequest.getHeader(headerName);
                                allHeaders.put(headerName, header);
                                if (headerName.startsWith("$DRILL_HEADER_PREFIX") && tomcatResponse.getHeader(headerName) == null) {
                                    tomcatResponse.addHeader(headerName, header);
                                }
                            }
                            com.epam.drill.request.HttpRequest.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                        }
                    """.trimIndent()
                )
                method.wrapCatching(
                    CtMethod::insertAfter,
                    """
                       com.epam.drill.request.PluginExtension.INSTANCE.${PluginExtension::processServerResponse.name}();
                    """.trimIndent()
                )
                return toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }
}
