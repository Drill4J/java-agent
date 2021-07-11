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
package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.core.callbacks.classloading.*
import com.epam.drill.core.callbacks.vminit.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.transport.common.ws.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*
import kotlin.test.*

@SharedImmutable
private val logger = Logging.logger("MainLogger")

private val LOGO = """
  ____    ____                 _       _          _  _                _      
 |  _"\U |  _"\ u     ___     |"|     |"|        | ||"|            U |"| u   
/| | | |\| |_) |/    |_"_|  U | | u U | | u      | || |_          _ \| |/    
U| |_| |\|  _ <       | |    \| |/__ \| |/__     |__   _|        | |_| |_,-. 
 |____/ u|_| \_\    U/| |\u   |_____| |_____|      /|_|\          \___/-(_/  
  |||_   //   \\_.-,_|___|_,-.//  \\  //  \\      u_|||_u          _//       
 (__)_) (__)  (__)\_)-' '-(_/(_")("_)(_")("_)     (__)__)         (__)  v. ${agentVersion}⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        """.trimIndent()

object Agent : JvmtiAgent {
    val isHttpHookEnabled: Boolean by lazy {
        getenv(SYSTEM_HTTP_HOOK_ENABLED)?.toKString()?.toBoolean() ?: memScoped {
            alloc<CPointerVar<ByteVar>>().apply {
                GetSystemProperty(HTTP_HOOK_ENABLED, this.ptr)
            }.value?.toKString()?.toBoolean() ?: true
        }
    }

    override fun agentOnLoad(options: String): Int {
        println(LOGO)
        try {
            val initialParams = agentParams(options)
            performAgentInitialization(initialParams)
            setUnhandledExceptionHook({ error: Throwable ->
                logger.error(error) { "unhandled event $error" }
            }.freeze())

            memScoped {
                val jvmtiCapabilities = alloc<jvmtiCapabilities>()
                jvmtiCapabilities.can_retransform_classes = 1.toUInt()
                jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
                AddCapabilities(jvmtiCapabilities.ptr)
            }
            AddToBootstrapClassLoaderSearch("$drillInstallationDir/drillRuntime.jar")
            callbackRegister()

            logger.info { "The native agent was loaded" }
            logger.info { "Pid is: " + getpid() }
        } catch (ex: Throwable) {
            logger.error { "Can't load the agent. Ex: ${ex.message}" }
            JNI_ERR
        }
        return JNI_OK
    }

    private fun agentParams(options: String): AgentParameters {
        logger.debug { "agent options:$options" }
        val agentParameters = options.asAgentParams()
        val configPath = agentParameters["configPath"] ?: getenv(SYSTEM_CONFIG_PATH)?.toKString()
        logger.debug { "configFile=$configPath, agent parameters:$agentParameters" }
        val agentParams = if (!configPath.isNullOrEmpty()) {
            val properties = readFile(configPath)
            logger.debug { "properties file:$properties" }
            properties.asAgentParams("\n", "#")
        } else {
            logger.warn { "Deprecated. You should use a config file instead of options. It will be removed in the next release" }
            agentParameters
        }
        logger.debug { "result of agent parameters:$agentParams" }
        return agentParams.validate()
    }

    override fun agentOnUnload() {
        logger.info { "Agent_OnUnload" }
    }
}

private fun String?.asAgentParams(
    lineDelimiter: String = ",",
    filterPrefix: String = "",
    mapDelimiter: String = "="
): AgentParameters {
    if (this.isNullOrEmpty()) return emptyMap()
    return try {
        this.split(lineDelimiter)
            .filter { it.isNotEmpty() && (filterPrefix.isEmpty() || !it.startsWith(filterPrefix)) }
            .associate {
                val (key, value) = it.split(mapDelimiter)
                key to value
            }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $this")
    }
}

private fun readFile(filePath: String): String {
    val fileDescriptor = open(filePath, EROFS)
    if (fileDescriptor == -1) throw IllegalArgumentException("Cannot open the config file with filePath='$filePath'")
    val bytes = Input(fileDescriptor).readBytes()
    return bytes.decodeToString()
}

private fun callbackRegister() = memScoped {
    val alloc = alloc<jvmtiEventCallbacks>()
    alloc.VMInit = staticCFunction(::jvmtiEventVMInitEvent)
    alloc.VMDeath = staticCFunction(::vmDeathEvent)
    alloc.ClassFileLoadHook = staticCFunction(::classLoadEvent)
    SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
}

@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    logger.info { "vmDeathEvent" }
}

typealias AgentParameters = Map<String, String>

private fun AgentParameters.validate(): AgentParameters = apply {
    check("agentId" in this) { "Please set 'agentId' as agent parameters e.g. -agentpath:/path/to/agent=agentId={your ID}" }
    val adminAddress = get("adminAddress")
    checkNotNull(adminAddress) { "Please set 'adminAddress' as agent parameters e.g. -agentpath:/path/to/agent=adminAddress={hostname:port}" }
    try {
        URL("ws://$adminAddress")
    } catch (parseException: RuntimeException) {
        fail("Please check 'adminAddress' parameter. It should be a valid address to the admin service without schema and any additional paths, e.g. localhost:8090")
    }
}
