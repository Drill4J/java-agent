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
package com.epam.drill.agent

import kotlin.native.concurrent.*
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.cinterop.*
import platform.posix.*
import mu.*
import com.epam.drill.agent.configuration.*
import com.epam.drill.agent.jvmti.event.*
import com.epam.drill.jvmapi.gen.*

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.agent.Agent")

private val LOGO = """
  ____    ____                 _       _          _  _                _      
 |  _"\U |  _"\ u     ___     |"|     |"|        | ||"|            U |"| u   
/| | | |\| |_) |/    |_"_|  U | | u U | | u      | || |_          _ \| |/    
U| |_| |\|  _ <       | |    \| |/__ \| |/__     |__   _|        | |_| |_,-. 
 |____/ u|_| \_\    U/| |\u   |_____| |_____|      /|_|\          \___/-(_/  
  |||_   //   \\_.-,_|___|_,-.//  \\  //  \\      u_|||_u          _//       
 (__)_) (__)  (__)\_)-' '-(_/(_")("_)(_")("_)     (__)__)         (__)  v. ${agentVersion}⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        """.trimIndent()

object Agent {

    val startTimeMark: TimeMark = TimeSource.Monotonic.markNow().freeze()

    val isHttpHookEnabled: Boolean by lazy {
        getenv(SYSTEM_HTTP_HOOK_ENABLED)?.toKString()?.toBoolean() ?: memScoped {
            alloc<CPointerVar<ByteVar>>().apply {
                GetSystemProperty(HTTP_HOOK_ENABLED, this.ptr)
            }.value?.toKString()?.toBoolean() ?: true
        }
    }

    fun agentOnLoad(options: String): Int {
        println(LOGO)

        defaultNativeLoggingConfiguration()
        val agentArguments = convertToAgentArguments(options)
        validate(agentArguments)
        performInitialConfiguration(agentArguments)
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

        return JNI_OK
    }

    fun agentOnUnload() {
        logger.info { "Agent_OnUnload" }
    }

    private fun callbackRegister() = memScoped {
        val alloc = alloc<jvmtiEventCallbacks>()
        alloc.VMInit = staticCFunction(::vmInitEvent)
        alloc.VMDeath = staticCFunction(::vmDeathEvent)
        alloc.ClassFileLoadHook = staticCFunction(::classFileLoadHook)
        SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
    }


}
