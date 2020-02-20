package com.epam.drill.agent.classloading

import java.net.*
import java.util.*

fun Map<String, ClassLoader>.loadClassData(): Map<String, ByteArray> = map { (resourceName, classLoader) ->
    val bytes = classLoader.url(resourceName).readBytes()
    val className = resourceName.removeSuffix(".class")
    className to bytes
}.toMap()


fun scanResourceMap(packagePrefixes: Iterable<String>): Map<String, ClassLoader> {
    val threadClassLoaders = Thread.getAllStackTraces().keys
        .mapNotNull(Thread::getContextClassLoader)
    val leafClassLoaders = threadClassLoaders
        .leaves(ClassLoader::getParent)
        .toListWith(ClassLoader.getSystemClassLoader())
    val classPath = ClassPath(packagePrefixes)
    return classPath.scan(leafClassLoaders)
}

private fun ClassLoader.url(resourceName: String): URL {
    return getResource(resourceName) ?: throw NoSuchElementException(resourceName)
}
