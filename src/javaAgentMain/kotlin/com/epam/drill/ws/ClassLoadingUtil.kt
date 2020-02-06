@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.*
import java.util.jar.*

object ClassLoadingUtil {

    fun retrieveApiClass(jarPath: String): Class<AgentPart<*, *>>? {
        var jf: JarFile? = null
        try {
            jf = JarFile(jarPath)
            @Suppress("UNCHECKED_CAST")
            return retrieveApiClass(
                AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
                ClassLoader.getSystemClassLoader()
            ) as Class<AgentPart<*, *>>?
        } finally {
            jf?.close()
        }
    }

    fun getPluginPayload(pluginId: String): PluginPayload = PluginPayload(pluginId, AgentPluginData)

    fun retrieveClassesData(config: String): String {
        val pp = PackagesPrefixes.serializer() parse config
        val scanItPlease = ClassPath().scanItPlease(ClassLoader.getSystemClassLoader())
        val filter = scanItPlease
            .filter { (classPath, _) ->
                classPath.endsWith(".class") && isTopLevelClass(classPath) && pp.packagesPrefixes.any { packageName ->
                    isAllowedClass(classPath, packageName)
                }
            }

        AgentPluginData.classMap = filter.map { (resourceName, classInfo) ->
            val className = resourceName
                .removePrefix("BOOT-INF/classes/")
                .removeSuffix(".class")
            val bytes = classInfo.url(resourceName).readBytes()
            className to bytes
        }.toMap()
        println("Agent loaded ${AgentPluginData.classMap.keys.count()} classes")

        val encodedClasses = AgentPluginData.classMap.map { (className, bytes) ->
            Base64Class.serializer() stringify Base64Class(className, bytes.encode())
        }

        return String.serializer().list stringify encodedClasses
    }

}
