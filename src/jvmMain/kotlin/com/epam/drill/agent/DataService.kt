@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.*
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
        val pp = PackagesPrefixes.serializer() parse config
        val scanItPlease = ClassPath().scanItPlease(ClassLoader.getSystemClassLoader())
        val filter = scanItPlease
            .filter { (classPath, _) ->
                classPath.isTopLevelClass && classPath.isAllowedFor(pp.packagesPrefixes)
            }.excludePackages("com/epam/drill")

        AgentPluginData.classMap = filter.map { (resourceName, classLoader) ->
            val className = resourceName
                .removePrefix("BOOT-INF/classes/")
                .removeSuffix(".class")
            val bytes = classLoader.url(resourceName).readBytes()
            className to bytes
        }.toMap()
        println("Agent loaded ${AgentPluginData.classMap.keys.count()} classes")

        val encodedClasses = AgentPluginData.classMap.map { (className, bytes) ->
            Base64Class.serializer() stringify Base64Class(className, bytes.encode())
        }

        return String.serializer().list stringify encodedClasses
    }
}
