package com.epam.drill.core.ws

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.concurrency.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.crypto.*
import com.epam.drill.logger.*
import com.epam.drill.ws.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlin.native.concurrent.*

@SharedImmutable
val wsLogger = DLogger("DrillWebsocket")

@SharedImmutable
val wsThread = Worker.start(true)

@SharedImmutable
val sendWorker = Worker.start(true)

@SharedImmutable
val loader = Worker.start(true)

@ThreadLocal
private val guaranteeQueue = LockFreeMPSCQueue<String>()


@ThreadLocal
private val binaryTopicsStorage = HashMap<PluginMetadata, PluginTopic>()

fun sendMessage(message: String) {
    sendWorker.execute(TransferMode.UNSAFE, { message }) {
        guaranteeQueue.addLast(it)
    }
}

const val DELAY = 3000L

fun startWs() {
    wsLogger.debug { "Starting WS" }

    wsThread.executeCoroutines {
        launch { topicRegister() }
        while (true) {
            delay(DELAY)
            try {
                runBlocking {
                    websocket(exec { adminAddress.toString() })
                }
            } catch (ex: Exception) {
                wsLogger.error { "Starting WS handle with exception '${ex.message}', try to reconnect" }
            }
        }
    }
}


suspend fun websocket(adminUrl: String) {
    val url = "$adminUrl/agent/attach"
    wsLogger.debug { "try to create websocket $url" }
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
        sendMessage(Message.serializer() stringify Message(MessageType.DEBUG, "", ""))
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
                    sendMessage(Message.serializer() stringify Message(MessageType.MESSAGE_DELIVERED, destination))
                }
                is GenericTopic<*> -> {
                    topic.deserializeAndRun(message.data)
                    sendMessage(Message.serializer() stringify Message(MessageType.MESSAGE_DELIVERED, destination))
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
    }

    wsClient.onError.add {
        wsLogger.error { "WS error: ${it.message}" }
    }
    wsClient.onClose.add {
        wsLogger.info { "Websocket closed" }
        wsClient.close()
        throw WsClosedException("")
    }

    coroutineScope {
        launch {
            while (true) {
                delay(50)
                val execute = sendWorker.execute(TransferMode.UNSAFE, {}) {
                    val first = guaranteeQueue.removeFirstOrNull()
                    first
                }.result
                if (execute != null) {
                    wsClient.send(execute)
                }
            }

        }
    }
}


private fun String.toWsMessage() = Message.serializer().parse(this)

fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

fun Worker.executeCoroutines(block: suspend CoroutineScope.() -> Unit): Future<Unit> {
    return this.execute(TransferMode.UNSAFE, { block }) {
        try {
            runBlocking {
                it(this)
            }
        } catch (ex: Throwable) {
            wsLogger.error { ex.message ?: "" }
        }
    }
}
