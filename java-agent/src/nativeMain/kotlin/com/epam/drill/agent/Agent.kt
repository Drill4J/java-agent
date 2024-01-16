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

import kotlin.native.concurrent.freeze
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import platform.posix.getpid
import mu.KotlinLogging
import com.epam.drill.agent.configuration.AgentLoggingConfiguration
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions.INSTALLATION_DIR
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.interceptor.HttpInterceptorConfigurer
import com.epam.drill.agent.jvmti.classFileLoadHook
import com.epam.drill.agent.jvmti.vmDeathEvent
import com.epam.drill.agent.jvmti.vmInitEvent
import com.epam.drill.agent.module.JvmModuleLoader
import com.epam.drill.agent.request.HeadersRetriever
import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.agent.transport.JvmModuleMessageSender
import com.epam.drill.jvmapi.gen.*

object Agent {

    private val logo = """
          ____    ____                 _       _          _  _                _      
         |  _"\U |  _"\ u     ___     |"|     |"|        | ||"|            U |"| u   
        /| | | |\| |_) |/    |_"_|  U | | u U | | u      | || |_          _ \| |/    
        U| |_| |\|  _ <       | |    \| |/__ \| |/__     |__   _|        | |_| |_,-. 
         |____/ u|_| \_\    U/| |\u   |_____| |_____|      /|_|\          \___/-(_/  
          |||_   //   \\_.-,_|___|_,-.//  \\  //  \\      u_|||_u          _//       
         (__)_) (__)  (__)\_)-' '-(_/(_")("_)(_")("_)     (__)__)         (__)  v. ${agentVersion}⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        """.trimIndent()

    private val logger = KotlinLogging.logger("com.epam.drill.agent.Agent")

    fun agentOnLoad(options: String): Int {
        println(logo)

        AgentLoggingConfiguration.defaultNativeLoggingConfiguration()
        Configuration.initializeNative(options)
        AgentLoggingConfiguration.updateNativeLoggingConfiguration()

        addCapabilities()
        setEventCallbacks()
        setUnhandledExceptionHook({ error: Throwable -> logger.error(error) { "Unhandled event: $error" }}.freeze())
        AddToBootstrapClassLoaderSearch("${Configuration.parameters[INSTALLATION_DIR]}/drillRuntime.jar")

        logger.info { "agentOnLoad: The native agent has been loaded" }
        logger.info { "agentOnLoad: Pid is: " + getpid() }

        return JNI_OK
    }

    fun agentOnUnload() {
        logger.info { "agentOnUnload" }
    }

    fun agentOnVmInit() {
        initRuntimeIfNeeded()
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

        AgentLoggingConfiguration.defaultJvmLoggingConfiguration()
        AgentLoggingConfiguration.updateJvmLoggingConfiguration()
        Configuration.initializeJvm()

        RequestHolder(Configuration.parameters[ParameterDefinitions.IS_ASYNC_APP])
        HttpInterceptorConfigurer(HeadersRetriever, RequestHolder)

        loadJvmModule("com.epam.drill.test2code.Test2Code")
        JvmModuleMessageSender.sendAgentMetadata()
    }

    fun agentOnVmDeath() {
        logger.info { "agentOnVmDeath" }
    }

    private fun addCapabilities() = memScoped {
        val jvmtiCapabilities = alloc<jvmtiCapabilities>()
        jvmtiCapabilities.can_retransform_classes = 1.toUInt()
        jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
        AddCapabilities(jvmtiCapabilities.ptr)
    }

    private fun setEventCallbacks() = memScoped {
        val alloc = alloc<jvmtiEventCallbacks>()
        alloc.VMInit = staticCFunction(::vmInitEvent)
        alloc.VMDeath = staticCFunction(::vmDeathEvent)
        alloc.ClassFileLoadHook = staticCFunction(::classFileLoadHook)
        SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
    }

    private fun loadJvmModule(clazz: String) = runCatching { JvmModuleLoader.loadJvmModule(clazz).load() }
        .onFailure { logger.error(it) { "loadJvmModule: Fatal error: id=${clazz}" } }

}
