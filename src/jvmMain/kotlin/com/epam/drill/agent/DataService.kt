@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.*
import java.util.*
import java.util.jar.*

object DataService {

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
        val packagesPrefixes = PackagesPrefixes.serializer() parse config
        val resourceMap = scanResourceMap(packagesPrefixes.packagesPrefixes)
        val loadedClassData = resourceMap.loadClassData()
        AgentPluginData.classMap = loadedClassData
        val encodedClasses = loadedClassData.map { (className, bytes) ->
            Base64Class.serializer() stringify Base64Class(className, Base64.getEncoder().encodeToString(bytes))
        }
        return String.serializer().list stringify encodedClasses
    }
}
