@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import mu.*
import java.util.jar.*
import kotlin.reflect.jvm.*
import kotlin.time.*

@Serializable
class ByteArrayListWrapper(val bytesList: List<ByteArray>)

@ExperimentalStdlibApi
actual object DataService {
    private val logger = KotlinLogging.logger(DataService::class.jvmName)

    fun retrieveApiClass(jarPath: String): Class<AgentPart<*, *>>? = JarFile(jarPath).use { jf ->
        val result = retrieveApiClass(
            AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
            ClassLoader.getSystemClassLoader()
        )
        @Suppress("UNCHECKED_CAST")
        result as Class<AgentPart<*, *>>?
    }

    fun getPluginPayload(pluginId: String): PluginPayload = PluginPayload(pluginId, AgentPluginData)

    actual fun retrieveClassesData(config: String): ByteArray {
        val packagesPrefixes = (PackagesPrefixes.serializer() parse config).packagesPrefixes

        logger.info { "Scanning classes, package prefixes: $packagesPrefixes..." }
        val scanResult = measureTimedValue { scanResourceMap(packagesPrefixes) }
        val classSources = scanResult.value
        logger.info { "Scanned ${classSources.count()} classes in  ${scanResult.duration}." }

        logger.info { "Loading ${classSources.count()} classes..." }
        val loadingResult = measureTimedValue {
            classSources.associate { it.className to it.bytes() }
        }
        val loadedClassData = loadingResult.value
        AgentPluginData.classMap = loadedClassData
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
}
