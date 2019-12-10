package com.epam.drill.core.ws

import com.epam.drill.*
import com.epam.drill.api.*
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
import kotlin.collections.set
import kotlin.native.concurrent.*

@SharedImmutable
val topicLogger = DLogger("topicLogger")

fun topicRegister() =
    WsRouter {
        topic<Communication.Agent.PluginLoadEvent>().withPluginTopic { pluginMeta, file ->
            if (exec { pstorage[pluginMeta.id] } != null) {
                topicLogger.info { "Plugin '${pluginMeta.id}' is already loaded" }
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

            }

        }


        topic<Communication.Agent.UpdateConfigEvent, ServiceConfig> { sc ->
            topicLogger.info { "Agent got a system config: $sc" }
            exec { secureAdminAddress = adminAddress.copy(scheme = "https", defaultPort = sc.sslPort.toInt()) }
        }
        topic<Communication.Agent.ChangeHeaderNameEvent> { headerName ->
            topicLogger.info { "Agent got a new headerMapping: $headerName" }
            exec { requestPattern = if (headerName.isEmpty()) null else headerName }
        }

        topic<Communication.Agent.SetPackagePrefixesEvent> { payload ->
            setPackagesPrefixes(payload)
            topicLogger.info { "Agent packages prefixes have been changed" }
        }

        topic<Communication.Agent.PluginUnloadEvent> { pluginId ->
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
        topic<Communication.Agent.LoadClassesDataEvent> {
            val base64Classes = getClassesByConfig()
            sendMessage(Message.serializer() stringify Message(MessageType.START_CLASSES_TRANSFER, "", ""))
            base64Classes.forEach {
                sendMessage(Message.serializer() stringify Message(MessageType.CLASSES_DATA, "", it))
            }
            sendMessage(Message.serializer() stringify Message(MessageType.FINISH_CLASSES_TRANSFER, "", ""))
            topicLogger.info { "Agent's application classes processing by config triggered" }
        }

        topic<Communication.Plugin.UpdateConfigEvent, PluginConfig> { config ->
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

        topic<Communication.Plugin.DispatchEvent, PluginAction> { m ->
            topicLogger.warn { "actionPluign event: message is ${m.message} " }
            val agentPluginPart = PluginManager[m.id]
            agentPluginPart?.doRawAction(m.message)

        }

        topic<Communication.Plugin.ToggleEvent, TogglePayload> { (pluginId, forceValue) ->
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