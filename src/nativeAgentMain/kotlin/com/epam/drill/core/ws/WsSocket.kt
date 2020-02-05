package com.epam.drill.core.ws

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.crypto.*
import com.epam.drill.logger.*
import com.epam.drill.transport.ws.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import mu.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

const val DELAY = 3000L

@SharedImmutable
private val wsLogger = KotlinLogging.logger("DrillWebsocket")

@ThreadLocal
private val binaryTopicsStorage = HashMap<PluginMetadata, PluginTopic>()

@SharedImmutable
private val dispatcher = newSingleThreadContext("sender coroutine")

class WsSocket : CoroutineScope {

    override val coroutineContext: CoroutineContext = dispatcher + CoroutineExceptionHandler { _, ex ->
        wsLogger.error { "WS error: ${ex.message}" }
        wsLogger.debug { "try reconnect" }
        connect(exec { adminAddress.toString() })
    }

    private val mainChannel = msChannel

    init {
        launch { topicRegister() }
    }

    fun connect(adminUrl: String) = launch {
        delay(DELAY)
        val url = "$adminUrl/agent/attach"
        wsLogger.debug { "try to create websocket $url" }
        process(url, mainChannel)
    }

    private suspend fun process(url: String, msChannel: Channel<String>) {
        val wsClient = RWebsocketClient(
            url = url,
            protocols = emptyList(),
            origin = "",
            wskey = "",
            params = mutableMapOf(
                AgentConfigParam to Cbor.dumps(AgentConfig.serializer(), exec { agentConfig }),
                NeedSyncParam to exec { agentConfig.needSync }.toString()
            )
        )
        wsClient.onOpen {
            wsLogger.debug { "Agent connected" }
        }

        wsClient.onAnyMessage.add {
            Sender.send(Message(MessageType.DEBUG, "", ""))
        }

        wsClient.onStringMessage.add { rawMessage ->
            val message = rawMessage.toWsMessage()
            val destination = message.destination
            val topic = WsRouter[destination]
            if (topic != null) {
                when (topic) {
                    is PluginTopic -> {
                        val pluginMetadata = PluginMetadata.serializer() parse message.data
                        binaryTopicsStorage[pluginMetadata] = topic
                    }
                    is InfoTopic -> {
                        topic.block(message.data)
                        Sender.send(Message(MessageType.MESSAGE_DELIVERED, destination))
                    }
                    is GenericTopic<*> -> {
                        topic.deserializeAndRun(message.data)
                        Sender.send(Message(MessageType.MESSAGE_DELIVERED, destination))
                    }
                }
            } else {
                wsLogger.warn { "topic with name '$destination' didn't register" }
            }

        }

        wsClient.onBinaryMessage.add { rawFile ->
            val md5FileHash = rawFile.md5().toHexString()
            wsLogger.info { "got '$md5FileHash' file to binary channel" }
            val metadata = binaryTopicsStorage.keys.first { it.md5Hash == md5FileHash }
            binaryTopicsStorage.remove(metadata)?.block?.invoke(metadata, rawFile) ?: run {
                wsLogger.warn { "can't find corresponded config fo'$md5FileHash' hash" }
            }
            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/load"))
        }

        wsClient.onError.add {
            wsLogger.error { "WS error: ${it.message}" }
        }
        wsClient.onClose.add {
            wsLogger.info { "Websocket closed" }
            wsClient.close()
            throw WsClosedException("")
        }
        while (true) wsClient.send(msChannel.receive())
    }

    fun close() {
        try {
            coroutineContext.cancelChildren()
        } catch (_: Exception) {
        }

    }
}

private fun String.toWsMessage() = Message.serializer().parse(this)

fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
