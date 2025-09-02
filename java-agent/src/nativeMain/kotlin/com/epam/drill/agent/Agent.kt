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
import com.epam.drill.agent.instrument.ApplicationClassTransformer
import com.epam.drill.agent.jvmti.classFileLoadHook
import com.epam.drill.agent.jvmti.vmDeathEvent
import com.epam.drill.agent.jvmti.vmInitEvent
import com.epam.drill.agent.module.JvmModuleLoader
import com.epam.drill.agent.transport.JvmModuleMessageSender
import com.epam.drill.agent.jvmapi.gen.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi
import com.epam.drill.agent.instrument.TransformerRegistrar
import com.epam.drill.agent.instrument.clients.ApacheHttpClientTransformer
import com.epam.drill.agent.instrument.clients.JavaHttpClientTransformer
import com.epam.drill.agent.instrument.clients.OkHttp3ClientTransformer
import com.epam.drill.agent.instrument.clients.SpringWebClientTransformer
import com.epam.drill.agent.instrument.jetty.Jetty10WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.Jetty11WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.Jetty9WsMessagesTransformer
import com.epam.drill.agent.instrument.jetty.JettyHttpServerTransformer
import com.epam.drill.agent.instrument.jetty.JettyWsClientTransformer
import com.epam.drill.agent.instrument.jetty.JettyWsServerTransformer
import com.epam.drill.agent.instrument.netty.NettyHttpServerTransformer
import com.epam.drill.agent.instrument.netty.NettyWsClientTransformer
import com.epam.drill.agent.instrument.netty.NettyWsMessagesTransformer
import com.epam.drill.agent.instrument.netty.NettyWsServerTransformer
import com.epam.drill.agent.instrument.servers.CadenceTransformer
import com.epam.drill.agent.instrument.servers.CompatibilityTestsTransformer
import com.epam.drill.agent.instrument.servers.KafkaTransformer
import com.epam.drill.agent.instrument.servers.ReactorTransformer
import com.epam.drill.agent.instrument.servers.SSLEngineTransformer
import com.epam.drill.agent.instrument.servers.TTLTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatHttpServerTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsClientTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsMessagesTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowHttpServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsClientTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsMessagesTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsServerTransformer

object Agent {

    private val logo = """
          ____    ____                 _       _          _  _                _      
         |  _"\U |  _"\ u     ___     |"|     |"|        | ||"|            U |"| u   
        /| | | |\| |_) |/    |_"_|  U | | u U | | u      | || |_          _ \| |/    
        U| |_| |\|  _ <       | |    \| |/__ \| |/__     |__   _|        | |_| |_,-. 
         |____/ u|_| \_\    U/| |\u   |_____| |_____|      /|_|\          \___/-(_/  
          |||_   //   \\_.-,_|___|_,-.//  \\  //  \\      u_|||_u          _//       
         (__)_) (__)  (__)\_)-' '-(_/(_")("_)(_")("_)     (__)__)         (__)  
         Java Agent (v${agentVersion})
        """.trimIndent()

    private val logger = KotlinLogging.logger("com.epam.drill.agent.Agent")
    private val transformers = setOf(
        ApplicationClassTransformer,
        TomcatHttpServerTransformer,
        JettyHttpServerTransformer,
        UndertowHttpServerTransformer,
        NettyHttpServerTransformer,
        JavaHttpClientTransformer,
        ApacheHttpClientTransformer,
        OkHttp3ClientTransformer,
        SpringWebClientTransformer,
        KafkaTransformer,
        CadenceTransformer,
        TTLTransformer,
        ReactorTransformer,
        SSLEngineTransformer,
        JettyWsClientTransformer,
        JettyWsServerTransformer,
        Jetty9WsMessagesTransformer,
        Jetty10WsMessagesTransformer,
        Jetty11WsMessagesTransformer,
        NettyWsClientTransformer,
        NettyWsServerTransformer,
        NettyWsMessagesTransformer,
        TomcatWsClientTransformer,
        TomcatWsServerTransformer,
        TomcatWsMessagesTransformer,
        UndertowWsClientTransformer,
        UndertowWsServerTransformer,
        UndertowWsMessagesTransformer,
        CompatibilityTestsTransformer,
    )

    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    fun agentOnLoad(options: String): Int {
        println(logo)
        AgentLoggingConfiguration.defaultNativeLoggingConfiguration()
        Configuration.initializeNative(options)
        AgentLoggingConfiguration.updateNativeLoggingConfiguration()
        TransformerRegistrar.initialize(transformers)

        addCapabilities()
        setEventCallbacks()
        setUnhandledExceptionHook({ error: Throwable -> logger.error(error) { "Unhandled event: $error" }}.freeze())
        AddToBootstrapClassLoaderSearch("${Configuration.parameters[INSTALLATION_DIR]}/drill-runtime.jar")

        logger.info { "agentOnLoad: Java Agent has been loaded. Pid is: " + getpid() }

        return JNI_OK
    }

    fun agentOnUnload() {
        logger.info { "agentOnUnload: Java Agent has been unloaded." }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun agentOnVmInit() {
        initRuntimeIfNeeded()
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

        AgentLoggingConfiguration.defaultJvmLoggingConfiguration()
        AgentLoggingConfiguration.updateJvmLoggingConfiguration()
        Configuration.initializeJvm()


        loadJvmModule("com.epam.drill.agent.test2code.Test2Code")
        JvmModuleMessageSender.sendAgentMetadata()
    }

    fun agentOnVmDeath() {
        logger.debug { "agentOnVmDeath" }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun addCapabilities() = memScoped {
        val jvmtiCapabilities = alloc<jvmtiCapabilities>()
        jvmtiCapabilities.can_retransform_classes = 1.toUInt()
        jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
        AddCapabilities(jvmtiCapabilities.ptr)
    }

    @OptIn(ExperimentalForeignApi::class)
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
