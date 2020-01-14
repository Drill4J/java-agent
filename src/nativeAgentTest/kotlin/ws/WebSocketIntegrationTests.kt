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
import kotlinx.serialization.cbor.*
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
        val agentConfig = Cbor.loads(AgentConfig.serializer(), rawAgentConfig)
        assertEquals(agentId, agentConfig.id)
        assertEquals("true", headersRequest.headers["needsync"])
    }


    @AfterTest
    fun serverShutdown() {
        close_socket(server.fd)
    }
}
