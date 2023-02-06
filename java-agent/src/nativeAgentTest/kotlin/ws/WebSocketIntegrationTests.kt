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
package ws

import Echo
import Echo.startServer
import TestBase
import com.epam.drill.common.*
import com.epam.drill.core.agent.*
import com.epam.drill.core.ws.*
import com.epam.drill.io.ktor.utils.io.internal.utils.test.*
import com.epam.drill.plugin.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

class WebSocketIntegrationTests : TestBase() {
    private val agentId = "test"
    private lateinit var server: Echo.SingleConnectServer


    @BeforeTest
    fun serverSetup() = runBlocking {
        server = startServer()
        performAgentInitialization(
            mapOf(
                "adminAddress" to "localhost:${server.port}",
                "agentId" to agentId,
                "drillInstallationDir" to "testDir"
            )
        )
    }

    @Test
    fun shouldExtendWsHeaders() = runTest {
        val wsSocket = WsSocket()
        wsSocket.connect("ws://localhost:${server.port}")
        val receive = server.accepted.receive()
        checkHeaders(receive.headers + "\n")
        wsSocket.close()
    }

    private fun checkHeaders(headers: String) {
        val headersRequest = parseHttpRequest(headers)
        val rawAgentConfig = headersRequest.headers["agentconfig"]
        assertNotNull(rawAgentConfig)
        val agentConfig = ProtoBuf.loads(AgentConfig.serializer(), rawAgentConfig)
        assertEquals(agentId, agentConfig.id)
        assertEquals("true", headersRequest.headers["needsync"])
    }


    @AfterTest
    fun serverShutdown() {
        close_socket(server.fd)
    }
}
