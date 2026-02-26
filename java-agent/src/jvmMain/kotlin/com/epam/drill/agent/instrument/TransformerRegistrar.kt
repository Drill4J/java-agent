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

import mu.KotlinLogging
import com.epam.drill.agent.instrument.clients.ApacheHttpClientTransformer
import com.epam.drill.agent.instrument.clients.JavaHttpClientTransformer
import com.epam.drill.agent.instrument.clients.OkHttp3ClientTransformer
import com.epam.drill.agent.instrument.clients.SpringWebClientTransformer
import com.epam.drill.agent.instrument.jetty.Jetty10WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.Jetty11WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.Jetty9WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.JettyHttpServerTransformer
import com.epam.drill.agent.instrument.jetty.JettyWsClientTransformer
import com.epam.drill.agent.instrument.jetty.JettyWsServerTransformer
import com.epam.drill.agent.instrument.netty.NettyHttpServerTransformer
import com.epam.drill.agent.instrument.netty.NettyWsClientTransformer
import com.epam.drill.agent.instrument.netty.NettyWsMessagesTransformer
import com.epam.drill.agent.instrument.netty.NettyWsServerTransformer
import com.epam.drill.agent.instrument.servers.CadenceTransformer
import com.epam.drill.agent.instrument.servers.CompatibilityTestsTransformer
import com.epam.drill.agent.instrument.servers.KafkaTransformer
import com.epam.drill.agent.instrument.servers.ReactorTransformer
import com.epam.drill.agent.instrument.servers.SSLEngineTransformer
import com.epam.drill.agent.instrument.servers.TTLTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatHttpServerTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsClientTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsMessagesTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowHttpServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsClientTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsMessagesTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsServerTransformer

actual object TransformerRegistrar {
    private val logger = KotlinLogging.logger {}
    private val transformers: Set<TransformerObject> = setOf(
        ApplicationClassTransformer,
        TomcatHttpServerTransformer,
        JettyHttpServerTransformer,
        UndertowHttpServerTransformer,
        NettyHttpServerTransformer,
        JavaHttpClientTransformer,
        ApacheHttpClientTransformer,
        OkHttp3ClientTransformer,
        SpringWebClientTransformer,
        KafkaTransformer,
        CadenceTransformer,
        TTLTransformer,
        ReactorTransformer,
        SSLEngineTransformer,
        JettyWsClientTransformer,
        JettyWsServerTransformer,
        Jetty9WsMessagesTransformer,
        Jetty10WsMessagesTransformer,
        Jetty11WsMessagesTransformer,
        NettyWsClientTransformer,
        NettyWsServerTransformer,
        NettyWsMessagesTransformer,
        TomcatWsClientTransformer,
        TomcatWsServerTransformer,
        TomcatWsMessagesTransformer,
        UndertowWsClientTransformer,
        UndertowWsServerTransformer,
        UndertowWsMessagesTransformer,
        CompatibilityTestsTransformer,
    )
    actual val enabledTransformers: List<Transformer> by lazy {
        transformers.filter { transformer ->
            transformer.enabled()
        }.also {
            logger.info { "Enabled ${it.size} jvm transformers" }
        }
    }
}