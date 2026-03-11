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
package com.epam.drill.agent.interceptor

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler

class JettyServerHttpInterceptorTest : AbstractHttpInterceptorTest() {

    override fun withHttpServer(block: (String) -> Unit) = Server().run {
        try {
            val connector = ServerConnector(this)
            this.connectors = arrayOf(connector)
            this.handler = TestRequestHandler
            this.start()
            block("http://localhost:${connector.localPort}")
        } finally {
            this.stop()
        }
    }

    @Suppress("VulnerableCodeUsages")
    private object TestRequestHandler : AbstractHandler() {
        override fun handle(
            target: String,
            baseRequest: Request,
            request: HttpServletRequest,
            response: HttpServletResponse
        ) {
            val requestBody = request.inputStream.readBytes()
            response.status = 200
            response.setContentLength(requestBody.size)
            response.outputStream.write(requestBody)
            response.outputStream.close()
        }
    }

}
