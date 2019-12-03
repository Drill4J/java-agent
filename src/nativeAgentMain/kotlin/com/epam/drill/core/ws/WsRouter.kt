package com.epam.drill.core.ws

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.*
import com.epam.drill.core.agent.*
import com.epam.drill.core.messanger.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.collections.set
import kotlin.native.concurrent.*

@SharedImmutable
val topicLogger = DLogger("topicLogger")

fun topicRegister() =
    WsRouter {

        topic("/agent/do-idle").rawMessage {
            topicLogger.info { "Change the state of the AGENT!" }
        }
        topic("/agent/do-busy").rawMessage {
            topicLogger.info { "Change the state of the AGENT!" }
        }
        topic(DrillEvent.SYNC_STARTED.name).rawMessage {
            topicLogger.info { "Agent synchronization is started" }
        }


        topic("/plugins/load").withPluginTopic { pluginMeta, file ->
            if (exec { pstorage[pluginMeta.id] } != null) {
                topicLogger.info { "Plugin '${pluginMeta.id}' is already loaded" }
                sendMessage(Message.serializer() stringify Message(MessageType.MESSAGE_DELIVERED, "/plugins/load"))
                return@withPluginTopic
            }
            val pluginId = pluginMeta.id
            exec { pl[pluginId] = pluginMeta }
            loader.execute(TransferMode.UNSAFE, { pluginMeta to file }) { (plugMessage, file) ->
                topicLogger.info { "try to load ${plugMessage.id} plugin" }
                val id = plugMessage.id
                exec { agentConfig.needSync = false }
                if (!plugMessage.isNative) runBlocking {
                    val path = generatePluginPath(id)
                    writeFileAsync(path, file)
                    loadPlugin(path, plugMessage)
                } else {
                    val natPlugin = generateNativePluginPath(id)

                    val loadNativePlugin = loadNativePlugin(
                        id,
                        natPlugin,
                        staticCFunction(::sendNativeMessage)
                    )
                    loadNativePlugin?.initPlugin()
                    loadNativePlugin?.on()
                }
                topicLogger.info { "$id plugin loaded" }

                sendMessage(Message.serializer() stringify Message(MessageType.MESSAGE_DELIVERED, "/plugins/load"))
            }

        }

        topic(DrillEvent.SYNC_FINISHED.name).rawMessage {
            exec { agentConfig.needSync = false }
            topicLogger.info { "Agent synchronization is finished" }
        }

        topic("/agent/load-classes-data").rawMessage {
            val base64Classes = getClassesByConfig()
            sendMessage(Message.serializer() stringify Message(MessageType.START_CLASSES_TRANSFER, "", ""))
            base64Classes.forEach {
                sendMessage(Message.serializer() stringify Message(MessageType.CLASSES_DATA, "", it))
            }
            sendMessage(Message.serializer() stringify Message(MessageType.FINISH_CLASSES_TRANSFER, "", ""))
            topicLogger.info { "Agent's application classes processing by config triggered" }
        }

        topic("/agent/set-packages-prefixes").rawMessage { payload ->
            setPackagesPrefixes(payload)
            topicLogger.info { "Agent packages prefixes have been changed" }
        }

        topic("/agent/config").withGenericTopic(ServiceConfig.serializer()) { sc ->
            topicLogger.info { "Agent got a system config: $sc" }
            exec { secureAdminAddress = adminAddress.copy(scheme = "https", defaultPort = sc.sslPort.toInt()) }
            exec { requestPattern = if(sc.headerName.isEmpty()) null else sc.headerName.toRegex() }
        }

        topic("/plugins/unload").rawMessage { pluginId ->
            topicLogger.warn { "Unload event. Plugin id is $pluginId" }
            PluginManager[pluginId]?.unload(UnloadReason.ACTION_FROM_ADMIN)
            println(
                """
                |________________________________________________________
                |Physical Deletion is not implemented yet.
                |We should unload all resource e.g. classes, jars, .so/.dll
                |Try to create custom classLoader. After this full GC.
                |________________________________________________________
            """.trimMargin()
            )
        }

        topic("/plugins/agent-attached").rawMessage {
            topicLogger.warn { "Agent is attached" }
        }

        topic("/plugins/updatePluginConfig").withGenericTopic(PluginConfig.serializer()) { config ->
            topicLogger.warn { "UpdatePluginConfig event: message is $config " }
            val agentPluginPart = PluginManager[config.id]
            if (agentPluginPart != null) {
                agentPluginPart.setEnabled(false)
                agentPluginPart.off()
                agentPluginPart.updateRawConfig(config)
                agentPluginPart.np?.updateRawConfig(config)
                agentPluginPart.setEnabled(true)
                agentPluginPart.on()
                topicLogger.warn { "New settings for ${config.id} saved to file" }
            } else
                topicLogger.warn { "Plugin ${config.id} not loaded to agent" }
        }

        topic("/plugins/action").withGenericTopic(PluginAction.serializer()) { m ->
            topicLogger.warn { "actionPluign event: message is ${m.message} " }
            val agentPluginPart = PluginManager[m.id]
            agentPluginPart?.doRawAction(m.message)

        }

        topic("/plugins/togglePlugin").withGenericTopic(TogglePayload.serializer()) { (pluginId, forceValue) ->
            val agentPluginPart = PluginManager[pluginId]
            if (agentPluginPart == null) {
                topicLogger.warn { "Plugin $pluginId not loaded to agent" }
            } else {
                topicLogger.warn { "togglePlugin event: PluginId is $pluginId" }
                val newValue = forceValue ?: !agentPluginPart.isEnabled()
                agentPluginPart.setEnabled(newValue)
                if (newValue) agentPluginPart.on() else agentPluginPart.off()
            }
        }

        topic("agent/toggleStandBy").rawMessage {
            topicLogger.warn { "toggleStandBy event" }
        }
    }

private fun generateNativePluginPath(id: String): String {
    //fixme do generate Native path
    return "$id/native_plugin.os_lib"
}

private fun generatePluginPath(id: String): String {
    val ajar = "agent-part.jar"
    val pluginsDir = "${if (tempPath.isEmpty()) drillInstallationDir else tempPath}/drill-plugins"
    doMkdir(pluginsDir)
    var pluginDir = "$pluginsDir/$id"
    doMkdir(pluginDir)
    pluginDir = "$pluginDir/${exec { agentConfig.id }}"
    doMkdir(pluginDir)
    val path = "$pluginDir/$ajar"
    return path
}


@ThreadLocal
object WsRouter {

    val mapper = mutableMapOf<String, Topic>()
    operator fun invoke(alotoftopics: WsRouter.() -> Unit) {
        alotoftopics(this)
    }


    @Suppress("ClassName")
    open class inners(open val destination: String) {
        fun <T> withGenericTopic(des: KSerializer<T>, block: suspend (T) -> Unit): GenericTopic<T> {
            val genericTopic = GenericTopic(destination, des, block)
            mapper[destination] = genericTopic
            return genericTopic
        }


        @Suppress("unused")
        fun withPluginTopic(block: suspend (message: PluginMetadata, file: ByteArray) -> Unit): PluginTopic {
            val fileTopic = PluginTopic(destination, block)
            mapper[destination] = fileTopic
            return fileTopic
        }


        fun rawMessage(block: suspend (String) -> Unit): InfoTopic {
            val infoTopic = InfoTopic(destination, block)
            mapper[destination] = infoTopic
            return infoTopic
        }

    }

    operator fun get(topic: String): Topic? {
        return mapper[topic]
    }

}

@Suppress("unused")
fun WsRouter.topic(url: String): WsRouter.inners {
    return WsRouter.inners(url)
}

open class Topic(open val destination: String)

class GenericTopic<T>(
    override val destination: String,
    private val deserializer: KSerializer<T>,
    val block: suspend (T) -> Unit
) : Topic(destination) {
    suspend fun deserializeAndRun(message: String) {
        block(deserializer parse message)
    }
}

class InfoTopic(
    override val destination: String,
    val block: suspend (String) -> Unit
) : Topic(destination)


open class PluginTopic(
    override val destination: String,
    @Suppress("unused") open val block: suspend (message: PluginMetadata, file: ByteArray) -> Unit
) : Topic(destination)

