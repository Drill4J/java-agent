@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import com.epam.drill.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.*
import java.util.jar.*
import kotlin.reflect.jvm.*
import kotlin.time.*

@Serializable
class ByteArrayListWrapper(val bytesList: List<ByteArray>)

@ExperimentalStdlibApi
@Kni
actual object DataService {
    private val logger = Logging.logger(DataService::class.jvmName)

    actual fun createAgentPart(id: String, jarPath: String): Any? = run {
        val agentPartClass = retrieveApiClass(jarPath)!!
        val constructor = agentPartClass.getConstructor(
            String::class.java,
            AgentContext::class.java,
            Sender::class.java,
            LoggerFactory::class.java
        )
        constructor.newInstance(id, RequestHolder.agentContext, PluginSender, Logging)
    }

    actual fun retrieveClassesData(config: String): ByteArray {
        val packagesPrefixes = Json.decodeFromString(PackagesPrefixes.serializer(), config).packagesPrefixes

        logger.info { "Scanning classes, package prefixes: $packagesPrefixes..." }
        val scanResult = measureTimedValue { scanResourceMap(packagesPrefixes) }
        val classSources = scanResult.value
        logger.info { "Scanned ${classSources.count()} classes in  ${scanResult.duration}." }

        logger.info { "Loading ${classSources.count()} classes..." }
        val loadingResult = measureTimedValue {
            classSources.associate { it.className to it.bytes() }
        }
        val loadedClassData = loadingResult.value
        val classCount = loadedClassData.count()
        logger.info { "Loaded $classCount classes in ${loadingResult.duration}" }

        logger.info { "Encoding $classCount classes..." }

        val encodingResult = measureTimedValue {
            val encodedClasses = loadedClassData.map { (className, bytes) ->
                ProtoBuf.dump(ByteClass.serializer(), ByteClass(className, bytes))
            }
            ProtoBuf.dump(ByteArrayListWrapper.serializer(), ByteArrayListWrapper(encodedClasses))
        }
        logger.info { "Encoded $classCount classes in ${encodingResult.duration}" }
        return encodingResult.value
    }

    actual fun doRawActionBlocking(
        agentPart: Any,
        data: String
    ): Any = with(agentPart as AgentPart<*>) {
        runBlocking { doRawAction(data) }
    }

    private fun retrieveApiClass(jarPath: String): Class<AgentPart<*>>? = JarFile(jarPath).use { jf ->
        val result = retrieveApiClass(
            AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
            ClassLoader.getSystemClassLoader()
        )
        @Suppress("UNCHECKED_CAST")
        result as Class<AgentPart<*>>?
    }
}
