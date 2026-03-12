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

import java.net.InetSocketAddress
import io.undertow.Undertow
import io.undertow.server.HttpServerExchange

class UndertowHttpInterceptorTest : AbstractHttpInterceptorTest() {

    override fun withHttpServer(block: (String) -> Unit) = Undertow.builder()
        .addHttpListener(0, "localhost")
        .setHandler(::testRequestHandler)
        .build().run {
            try {
                this.start()
                block("http://localhost:${(this.listenerInfo[0].address as InetSocketAddress).port}")
            } finally {
                this.stop()
            }
        }

    private fun testRequestHandler(exchange: HttpServerExchange) {
        lateinit var requestBody: ByteArray
        exchange.requestReceiver.receiveFullBytes { _, body -> requestBody = body}
        exchange.statusCode = 200
        exchange.responseContentLength = requestBody.size.toLong()
        exchange.responseSender.send(requestBody.decodeToString())
        exchange.responseSender.close()
    }

}
