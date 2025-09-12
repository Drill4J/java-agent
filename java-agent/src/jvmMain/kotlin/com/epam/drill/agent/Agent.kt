package com.epam.drill.agent

import com.epam.drill.agent.common.configuration.AgentMetadata
import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.configuration.Configuration
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
import com.epam.drill.agent.module.JvmModuleLoader
import com.epam.drill.agent.test2code.Test2Code
import com.epam.drill.agent.transport.HttpAgentMessageDestinationMapper
import com.epam.drill.agent.transport.JsonAgentMessageSerializer
import com.epam.drill.agent.transport.JvmModuleMessageSender
import com.epam.drill.agent.transport.SimpleAgentMessageSender
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import jdk.internal.org.objectweb.asm.ClassReader
import mu.KotlinLogging
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation

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
//    SSLEngineTransformer,
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
    println(logo)
    Configuration.initializeJvm(agentArgs ?: "")
    TransformerRegistrar.initialize(transformers)
    inst.addTransformer(DrillClassLoadTransformer, true)
    JvmModuleMessageSender.sendAgentMetadata()
    JvmModuleLoader.loadJvmModule(Test2Code::class.java.name)
}

object DrillClassLoadTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: java.security.ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        // Implement transformation logic here if needed
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