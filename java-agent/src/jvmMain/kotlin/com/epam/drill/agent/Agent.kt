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

import com.epam.drill.agent.configuration.AgentParameterValidationError
import com.epam.drill.agent.configuration.AgentParametersValidator
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.instrument.ApplicationClassTransformer
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
import com.epam.drill.agent.instrument.servers.TTLTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatHttpServerTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsClientTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsMessagesTransformer
import com.epam.drill.agent.instrument.tomcat.TomcatWsServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowHttpServerTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsClientTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsMessagesTransformer
import com.epam.drill.agent.instrument.undertow.UndertowWsServerTransformer
import com.epam.drill.agent.logging.LoggingConfiguration
import com.epam.drill.agent.module.JvmModuleLoader
import com.epam.drill.agent.test2code.Test2Code
import com.epam.drill.agent.test2code.configuration.Test2CodeParameterDefinitions
import com.epam.drill.agent.transport.JvmModuleMessageSender
import org.objectweb.asm.ClassReader
import mu.KotlinLogging
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import kotlin.system.exitProcess

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
private const val DRILL_PACKAGE = "com/epam/drill/agent"

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
//    SSLEngineTransformer, TODO does not work in JVM due to too early initialization of HeadersRetriever
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

fun premain(agentArgs: String?, inst: Instrumentation) {
    try {
        println(logo)
        LoggingConfiguration.readDefaultConfiguration()
        Configuration.initializeNative(agentArgs ?: "")
        updateJvmLoggingConfiguration()
        validateConfiguration()
        TransformerRegistrar.initialize(transformers)
        inst.addTransformer(DrillClassFileTransformer, true)
        JvmModuleMessageSender.sendAgentMetadata()
        JvmModuleLoader.loadJvmModule(Test2Code::class.java.name).load()
    } catch (e: Throwable) {
        println("Drill4J Initialization Error:\n${e.message ?: e::class.java.name}")
    }
}

fun main(args: Array<String>) {
    try {
        println(logo)
        LoggingConfiguration.readDefaultConfiguration()
        Configuration.initializeNative(args.convertToAgentArgs())
        updateJvmLoggingConfiguration()
        validateConfiguration()

        val commitSha = Configuration.parameters[DefaultParameterDefinitions.COMMIT_SHA]
        val buildVersion = Configuration.parameters[DefaultParameterDefinitions.BUILD_VERSION]
        if (commitSha == null && buildVersion == null)
            throw AgentParameterValidationError("Either commitSha or buildVersion must be provided")

        JvmModuleMessageSender.sendBuildMetadata()
        val test2Code = JvmModuleLoader.loadJvmModule(Test2Code::class.java.name) as Test2Code
        test2Code.scanAndSendMetadataClasses()
        Runtime.getRuntime().addShutdownHook(Thread { JvmModuleMessageSender.shutdown() })
        exitProcess(0)
    } catch (e: Throwable) {
        println("Drill4J Initialization Error:\n${e.message ?: e::class.java.name}")
        exitProcess(1)
    }
}

private fun validateConfiguration() {
    val validator = AgentParametersValidator(Configuration.parameters)
    validator.validate(
        DefaultParameterDefinitions,
        ParameterDefinitions,
        Test2CodeParameterDefinitions
    )
}

private fun Array<String>.convertToAgentArgs(): String = this
    .filter { it.startsWith("--") && it.contains("=") }
    .associate {
        val (key, value) = it.removePrefix("--").split("=", limit = 2)
        key to value
    }.filter { it.value.isNotEmpty() }
    .map { "${it.key}=${it.value}" }
    .joinToString(",")

private fun updateJvmLoggingConfiguration() {
    val logLevel = Configuration.parameters[ParameterDefinitions.LOG_LEVEL]
    val logFile = Configuration.parameters[ParameterDefinitions.LOG_FILE]
    val logLimit = Configuration.parameters[ParameterDefinitions.LOG_LIMIT]

    LoggingConfiguration.setLoggingLevels(logLevel)
    if (LoggingConfiguration.getLoggingFilename() != logFile) {
        LoggingConfiguration.setLoggingFilename(logFile)
    }
    if (LoggingConfiguration.getLogMessageLimit() != logLimit) {
        LoggingConfiguration.setLogMessageLimit(logLimit)
    }
}

object DrillClassFileTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: java.security.ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        val kClassName = className ?: return null
        val kClassBytes = classfileBuffer ?: return null
        val precheckedTransformers = TransformerRegistrar.enabledTransformers
            .filterNot { kClassName.startsWith(DRILL_PACKAGE) }
            .filter { it.precheck(kClassName, loader, protectionDomain) }
            .takeIf { it.any() }
            ?: return null
        val (oldClassBytes, reader) = runCatching {
            kClassBytes to ClassReader(kClassBytes)
        }.onFailure {
            logger.error(it) { "Can't read class: $kClassName" }
        }.getOrNull() ?: return null
        val permittedTransformers = precheckedTransformers.filter {
            it.permit(
                kClassName,
                reader.superName,
                reader.interfaces
            )
        }

        val newClassBytes = permittedTransformers.fold(oldClassBytes) { bytes, transformer ->
            runCatching {
                transformer.transform(kClassName, bytes, loader, protectionDomain)
            }.onFailure {
                logger.warn(it) { "Can't transform class: $kClassName with ${transformer::class.simpleName}" }
            }.getOrNull()
                ?.takeIf { it !== bytes }
                ?.also {
                    logger.debug { "$kClassName was transformed by ${transformer::class.simpleName}" }
                } ?: bytes
        }

        return if (newClassBytes !== oldClassBytes) newClassBytes else null
    }
}