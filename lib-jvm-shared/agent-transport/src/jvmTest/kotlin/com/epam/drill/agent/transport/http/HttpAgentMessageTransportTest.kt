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

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.entity.GzipCompressingEntity
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.ResponseStatus
import io.mockk.slot
import kotlin.test.*

class HttpAgentMessageTransportTest {

    @MockK
    private lateinit var clientBuilder: HttpClientBuilder
    @MockK
    private lateinit var closeableHttpClient: CloseableHttpClient

    private val request = slot<ClassicHttpRequest>()

    @BeforeTest
    fun setup() = MockKAnnotations.init(this).also {
        every { clientBuilder.build() } returns closeableHttpClient
        every { closeableHttpClient.close() } returns Unit
        every {
            closeableHttpClient.execute(capture(request), any<HttpClientResponseHandler<ResponseStatus<ByteArray>>>())
        } returns ResponseStatus(true)
    }

    @Test
    fun `successful GET`() = withHttpClientBuilder {
        val transport = HttpAgentMessageTransport("http://someadmin", "")
        val destination = AgentMessageDestination("GET", "somepath")
        val status = transport.send(destination, ByteArray(2), "mime/type").success

        assertTrue(status)
        verifyClassicHttpRequest<HttpGet>("http://someadmin/somepath", "mime/type")
    }

    @Test
    fun `successful POST`() = withHttpClientBuilder {
        val transport = HttpAgentMessageTransport("http://someadmin", "")
        val destination = AgentMessageDestination("POST", "somepath")
        val status = transport.send(destination, ByteArray(2), "mime/type").success

        assertTrue(status)
        verifyClassicHttpRequest<HttpPost>("http://someadmin/somepath", "mime/type")
    }

    @Test
    fun `successful PUT`() = withHttpClientBuilder {
        val transport = HttpAgentMessageTransport("http://someadmin", "")
        val destination = AgentMessageDestination("PUT", "somepath")
        val status = transport.send(destination, ByteArray(2), "mime/type").success

        assertTrue(status)
        verifyClassicHttpRequest<HttpPut>("http://someadmin/somepath", "mime/type")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown HTTP method`() = withHttpClientBuilder {
        val transport = HttpAgentMessageTransport("http://someadmin", "")
        val destination = AgentMessageDestination("SOME", "somepath")
        transport.send(destination, ByteArray(2))
    }

    @Test
    fun `default content type`() = withHttpClientBuilder {
        val transport = HttpAgentMessageTransport("http://someadmin", "")
        val destination = AgentMessageDestination("POST", "somepath")
        val status = transport.send(destination, ByteArray(2)).success

        assertTrue(status)
        verifyClassicHttpRequest<HttpPost>("http://someadmin/somepath", "*/*")
    }

    private inline fun withHttpClientBuilder(block: () -> Unit) = mockkStatic(HttpClientBuilder::class) {
        every { HttpClientBuilder.create() } returns clientBuilder
        block()
    }

    private inline fun <reified T : ClassicHttpRequest> verifyClassicHttpRequest(uri: String, contentType: String) {
        verify(exactly = 1) {
            closeableHttpClient.execute(any(), any<HttpClientResponseHandler<ResponseStatus<ByteArray>>>())
        }
        verify(exactly = 1) {
            closeableHttpClient.execute(request.captured, any<HttpClientResponseHandler<ResponseStatus<ByteArray>>>())
        }
        assertIs<T>(request.captured)
        assertIs<GzipCompressingEntity>(request.captured.entity)
        assertEquals(uri, request.captured.uri.toString())
        assertEquals(contentType, request.captured.getHeader(HttpHeaders.CONTENT_TYPE).value)
        assertEquals(contentType, request.captured.entity.contentType)
        assertEquals("gzip", request.captured.entity.contentEncoding)
    }

}
