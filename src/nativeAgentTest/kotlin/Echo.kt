import com.epam.drill.core.concurrency.*
import com.epam.drill.internal.socket.*
import com.epam.drill.internal.socket.socket_get_error
import com.epam.drill.io.ktor.utils.io.internal.utils.test.kx_init_sockets
import com.epam.drill.io.ktor.utils.io.internal.utils.test.make_socket_non_blocking
import com.epam.drill.transport.net.*
import com.epam.drill.transport.ws.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.internal.utils.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.posix.*
import kotlin.test.*


object Echo {
    data class Client(val fd: KX_SOCKET, val headers: String, val cln: NativeAsyncServer) {
        suspend fun close() {
            cln.sendWsFrame(WsFrame(byteArrayOf(), WsOpcode.Close))
            close(cln.socket.sockfds.toULong())
        }
    }

    data class SingleConnectServer(val fd: KX_SOCKET, val port: Int, val accepted: Channel<Client> = Channel())

    suspend fun startServer() = memScoped {
        kx_init_sockets()
        val serverAddr = alloc<sockaddr_in>()
        with(serverAddr) {
            memset(this.ptr, 0, sockaddr_in.size.convert())
            sin_family = AF_INET.convert()
            sin_port = htons(0.toUShort())
        }

        val acceptor = bind(serverAddr)
        val addrSizeResult = alloc<UIntVar>()
        addrSizeResult.value = sockaddr_in.size.convert()
        getsockname(
            acceptor, serverAddr.ptr.reinterpret(),
            addrSizeResult.ptr.reinterpret()
        ).checkError("getsockname()")
        val server = SingleConnectServer(acceptor, htons(serverAddr.sin_port).toInt())
        BackgroundThread {
            while (true) {
                val accepted: KX_SOCKET = accept(acceptor)
                delay(100)
                setup_buffer_size(accepted)
                val processHeaders = processHeaders(accepted)
                val nativeSocket = NativeSocketServer(accepted)
                val nativeAsyncServer = NativeAsyncServer(nativeSocket)
                BackgroundThread {
                    server.accepted.send(Client(accepted, processHeaders, nativeAsyncServer))
                }
                BackgroundThread {
                    while (true) {
                        val frame = nativeAsyncServer.readWsFrame()
                        try {
                            when (frame.type) {
                                WsOpcode.Close -> {
                                    nativeAsyncServer.sendWsFrame(WsFrame(byteArrayOf(), WsOpcode.Close))
                                }
                                WsOpcode.Ping -> {
                                    nativeAsyncServer.sendWsFrame(WsFrame(frame.data, WsOpcode.Pong))
                                }
                                WsOpcode.Pong -> {
                                }
                                else -> {
                                    nativeAsyncServer.sendWsFrame(frame)
                                }
                            }
                        } catch (ex: Exception) {
                            break
                        }
                    }
                }
            }
        }
        server
    }

    private suspend fun accept(acceptor: KX_SOCKET): KX_SOCKET {
        val zero: KX_SOCKET = 0.convert()
        var accepted: KX_SOCKET = zero
        while (accepted == zero) {
            delay(500)
            val result = accept(acceptor, null, null)

            if (result < zero || result == KX_SOCKET.MAX_VALUE) {
                val error = socket_get_error()
                if (error != EAGAIN && error != EWOULDBLOCK) {
                    fail("accept(): ${socket_get_error()}")
                }
            } else {
                accepted = result
                accepted.makeNonBlocking()
            }

        }
        return accepted
    }

    private fun processHeaders(accepted: KX_SOCKET): String {
        val buffer = ByteArray(2048)
        val offset = 0
        //read connect headers
        val headersSize = recv(accepted, buffer.refTo(offset), 2048, 0.convert())
        val headers = buffer.copyOfRange(0, headersSize.toInt() - 1).decodeToString()
        val message = "101 OK\n\n".toByteArray()
        send(accepted,  message.refTo(0), message.size.convert(), 0)
        return headers
    }

    private suspend fun bind(serverAddr: sockaddr_in): KX_SOCKET {
        val acceptor: KX_SOCKET = socket(AF_INET, SOCK_STREAM, 0).checkError("socket()")
        setup_buffer_size(acceptor)
        acceptor.makeNonBlocking()
        bind(acceptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert()).let { rc ->
            if (rc != 0) {
                delay(1000)
                bind(serverAddr)
            }
        }
        listen(acceptor, 10).checkError("listen()")
        return acceptor
    }


    private fun KX_SOCKET.makeNonBlocking() {
        make_socket_non_blocking(this)
    }

    @Suppress("unused")
    internal fun Int.checkError(action: String = ""): Int = when {
        this < 0 -> memScoped { fail(action) }
        else -> this
    }

    @Suppress("unused")
    internal fun Long.checkError(action: String = ""): Long = when {
        this < 0 -> memScoped { fail(action) }
        else -> this
    }

    private val ZERO: size_t = 0u

    @Suppress("unused")
    internal fun size_t.checkError(action: String = ""): size_t = when (this) {
        ZERO -> errno.let { errno ->
            when (errno) {
                0 -> this
                else -> memScoped { fail(action) }
            }
        }
        else -> this
    }

    private fun htons(value: UShort): uint16_t = when (ByteOrder.BIG_ENDIAN) {
        ByteOrder.nativeOrder() -> value
        else -> value.toShort().reverseByteOrder().toUShort()
    }

}