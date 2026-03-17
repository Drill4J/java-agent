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
package com.epam.drill.agent.transport.http

import java.io.File
import java.net.URI
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.entity.GzipCompressingEntity
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.ssl.SSLContextBuilder
import mu.KotlinLogging
import com.epam.drill.agent.transport.AgentMessageTransport
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.ResponseStatus
import org.apache.hc.core5.http.HttpStatus

private const val HEADER_DRILL_INTERNAL = "drill-internal"
private const val HEADER_API_KEY = "X-Api-Key"

class HttpAgentMessageTransport(
    serverAddress: String,
    apiKey: String = "",
    sslTruststore: String = "",
    sslTruststorePass: String = "",
    drillInternal: Boolean = true,
    private val gzipCompression: Boolean = true,
) : AgentMessageTransport {

    private val logger = KotlinLogging.logger {}
    private val clientBuilder = HttpClientBuilder.create()
    private val serverUri = URI(serverAddress)
    private val drillInternalHeader = drillInternal.takeIf(true::equals)?.let { BasicHeader(HEADER_DRILL_INTERNAL, it) }
    private val apiKeyHeader = apiKey.takeIf(String::isNotBlank)?.let { BasicHeader(HEADER_API_KEY, it) }
    private val contentTypes = mutableMapOf<String, ContentType>()

    init {
        logger.debug { "configure: Using serverAddress: $serverUri" }
        if (serverUri.scheme == "https") {
            val configureTrustStore: (SSLContextBuilder) -> Unit = {
                if (sslTruststore.isEmpty()) it.loadTrustMaterial { _, _ -> true }
                else it.loadTrustMaterial(File(sslTruststore), sslTruststorePass.toCharArray())
            }
            val configureHostnameVerifier: (SSLConnectionSocketFactoryBuilder) -> Unit = {
                if (sslTruststore.isEmpty()) it.setHostnameVerifier(NoopHostnameVerifier())
            }
            val sslContext = SSLContextBuilder.create()
                .also(configureTrustStore).build()
            val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext)
                .also(configureHostnameVerifier).build()
            val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory).build()
            clientBuilder.setConnectionManager(connectionManager)
            clientBuilder.setConnectionManagerShared(true)
            logger.debug { "constructor: SSL configured, truststore: $sslTruststore" }
            logger.debug { "constructor: SSL configured, trustAll: ${sslTruststore.isEmpty()}" }
        }
    }

    override fun send(
        destination: AgentMessageDestination,
        message: ByteArray?,
        contentType: String
    ): ResponseStatus<ByteArray> = clientBuilder.build().use { client ->
        val request = when (destination.type) {
            "GET" -> HttpGet(serverUri.resolve(destination.target))
            "POST" -> HttpPost(serverUri.resolve(destination.target))
            "PUT" -> HttpPut(serverUri.resolve(destination.target))
            "DELETE" -> HttpDelete(serverUri.resolve(destination.target))
            else -> throw IllegalArgumentException("Unknown destination type: ${destination.type}")
        }
        val mimeType = contentType.takeIf(String::isNotEmpty) ?: ContentType.WILDCARD.mimeType
        drillInternalHeader?.also(request::setHeader)
        apiKeyHeader?.also(request::setHeader)
        request.setHeader(HttpHeaders.CONTENT_TYPE, mimeType)
        if (message != null) {
            request.entity = ByteArrayEntity(message, getContentType(mimeType))
            if (gzipCompression) {
                request.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip")
                request.entity = GzipCompressingEntity(request.entity)
            }
        }
        logger.trace {
            val messageAsString = message?.decodeToString()
            "execute: Request to ${request.uri}, method: ${request.method}, request body: $messageAsString"
        }

        try {
            client.execute(request, ::contentResponseHandler)
        } catch (e: Throwable) {
            ResponseStatus(success = false, errorContent = e.message)
        }.onSuccess { content ->
            logger.debug {
                val messageAsString = content?.decodeToString()
                "execute: Successful response from ${request.uri}, method: ${request.method}, response body: $messageAsString"
            }
        }.onError { errorContent ->
            logger.debug {
                "execute: Failed response from ${request.uri}, method: ${request.method}, error message: $errorContent"
            }
        }
    }

    private fun contentResponseHandler(response: ClassicHttpResponse) = ResponseStatus(
        success = isSuccess(response),
        content = response.takeIf(::isSuccess)?.let { EntityUtils.toByteArray(it.entity) },
        errorContent = response.takeIf(::isFail)?.let { EntityUtils.toString(it.entity) },
    )

    private fun getContentType(mimeType: String) = contentTypes.getOrPut(mimeType) { ContentType.create(mimeType) }
    private fun isSuccess(response: ClassicHttpResponse) = response.code == HttpStatus.SC_SUCCESS
    private fun isFail(response: ClassicHttpResponse) = !isSuccess(response)
}
