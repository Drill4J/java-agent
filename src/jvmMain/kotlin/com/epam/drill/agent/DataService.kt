@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.logging.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.builtins.*
import java.util.*
import java.util.jar.*
import java.util.logging.*
import kotlin.time.*

object DataService {
    private val log = Logger.getLogger(DataService::class.java.name)

    fun retrieveApiClass(jarPath: String): Class<AgentPart<*, *>>? = JarFile(jarPath).use { jf ->
        val result = retrieveApiClass(
            AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
            ClassLoader.getSystemClassLoader()
        )
        @Suppress("UNCHECKED_CAST")
        result as Class<AgentPart<*, *>>?
    }

    fun getPluginPayload(pluginId: String): PluginPayload = PluginPayload(pluginId, AgentPluginData)

    fun retrieveClassesData(config: String): String {
        val packagesPrefixes = (PackagesPrefixes.serializer() parse config).packagesPrefixes

        log(Level.INFO) { "Scanning classes, package prefixes: $packagesPrefixes..." }
        val scanResult = measureTimedValue { scanResourceMap(packagesPrefixes) }
        val classSources = scanResult.value
        log(Level.INFO) { "Scanned ${classSources.count()} classes in  ${scanResult.duration}." }

        log(Level.INFO) { "Loading ${classSources.count()} classes..." }
        val loadingResult = measureTimedValue {
            classSources.associate { it.className to it.bytes() }
        }
        val loadedClassData = loadingResult.value
        AgentPluginData.classMap = loadedClassData
        val classCount = loadedClassData.count()
        log(Level.INFO) { "Loaded $classCount classes in ${loadingResult.duration}" }

        log(Level.INFO) { "Encoding $classCount classes..." }
        val encodingResult = measureTimedValue {
            val encodedClasses = loadedClassData.map { (className, bytes) ->
                Base64Class.serializer() stringify Base64Class(className, Base64.getEncoder().encodeToString(bytes))
            }
            String.serializer().list stringify encodedClasses
        }
        log(Level.INFO) { "Encoded $classCount classes in ${encodingResult.duration}" }

        return encodingResult.value
    }
}
