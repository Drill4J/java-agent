/**
 * Copyright 2020 EPAM Systems
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

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object TomcatTransformer {
    private val logger = Logging.logger(Transformer::class.jvmName)

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?
    ): ByteArray? {
        return try {
            val adminUrl = retrieveAdminAddress().decodeToString()
            logger.info { "starting TomcatTransformer with admin host $adminUrl..." }
            ClassPool.getDefault().appendClassPath(LoaderClassPath(loader as? ClassLoader))
            ClassPool.getDefault().makeClass(ByteArrayInputStream(classFileBuffer))?.run {
                val drillAdminHeader = """drill-admin-url"""
                getMethod(
                    "doFilter",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"
                )?.insertBefore(
                    """
                        if ($1 instanceof org.apache.catalina.connector.RequestFacade) {
                            org.apache.catalina.connector.ResponseFacade tomcatResponse = (org.apache.catalina.connector.ResponseFacade)$2;
                            if (tomcatResponse.getHeader("$drillAdminHeader") != "$adminUrl") {
                                tomcatResponse.addHeader("$drillAdminHeader", "$adminUrl");
                                tomcatResponse.addHeader("${idHeaderConfigKey().decodeToString()}", "${idHeaderConfigValue().decodeToString()}");
                            }
                            
                            org.apache.catalina.connector.RequestFacade tomcatRequest = (org.apache.catalina.connector.RequestFacade)${'$'}1;
                            java.util.Map/*<java.lang.String, java.lang.String>*/ allHeaders = new java.util.HashMap();
                            java.util.Enumeration/*<String>*/ headerNames = tomcatRequest.getHeaderNames();
                            while (headerNames.hasMoreElements()) {
                                java.lang.String headerName = (java.lang.String) headerNames.nextElement();
                                java.lang.String header = tomcatRequest.getHeader(headerName);
                                allHeaders.put(headerName, header);
                                if (headerName.startsWith("drill-")) {
                                    tomcatResponse.addHeader(headerName, header);
                                }
                            }
                            com.epam.drill.request.HttpRequest.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                        }
                    """.trimIndent()
                ) ?: run {
                    return null
                }
                return toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }

    actual external fun retrieveAdminAddress(): ByteArray
    actual external fun idHeaderConfigKey(): ByteArray
    actual external fun idHeaderConfigValue(): ByteArray
}
