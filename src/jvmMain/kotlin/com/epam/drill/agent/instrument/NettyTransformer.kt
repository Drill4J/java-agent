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

import com.epam.drill.agent.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import javassist.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object NettyTransformer {
    val logger = Logging.logger(NettyTransformer::class.jvmName)

    private val DefaultHttpRequest = "io.netty.handler.codec.http.DefaultHttpRequest"
    private val DefaultHttpResponse = "io.netty.handler.codec.http.DefaultHttpResponse"

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?
    ): ByteArray? {
        return try {
            ClassPool.getDefault().appendClassPath(LoaderClassPath(loader as? ClassLoader))
            ClassPool.getDefault().makeClass(ByteArrayInputStream(classFileBuffer))?.run {
                getMethod("invokeChannelRead", "(Ljava/lang/Object;)V").wrapCatching(
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
                            ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(allHeaders);
                        }
                    """.trimIndent()
                )
                val drillAdminHeader = BasicResponseHeaders.adminAddressHeader()
                val adminUrl = BasicResponseHeaders.retrieveAdminAddress()
                val writeMethod = getMethod("write", "(Ljava/lang/Object;ZLio/netty/channel/ChannelPromise;)V")
                writeMethod.wrapCatching(
                    CtMethod::insertBefore,
                    """
                        if ($1 instanceof $DefaultHttpResponse) {
                            $DefaultHttpResponse nettyResponse = ($DefaultHttpResponse) $1;
                            if (!"$adminUrl".equals(nettyResponse.headers().get("$drillAdminHeader"))) {
                                nettyResponse.headers().add("$drillAdminHeader", "$adminUrl");
                                nettyResponse.headers().add("${BasicResponseHeaders.idHeaderConfigKey()}", "${BasicResponseHeaders.idHeaderConfigValue()}");
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
                    """.trimIndent()
                )
                writeMethod.wrapCatching(
                    CtMethod::insertAfter,
                    """
                        if ($1 instanceof $DefaultHttpResponse) {
                            ${PluginExtension::class.java.name}.INSTANCE.${PluginExtension::processServerResponse.name}();
                        }
                    """.trimIndent()
                )
                toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error. Reason:" }
            null
        }
    }
}
