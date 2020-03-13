package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*

fun scanResourceMap(packagePrefixes: Iterable<String>): List<ClassSource> {
    return scanAvailableClassLoaders(packagePrefixes) + scanExternalSources(packagePrefixes)
}

fun scanExternalSources(packagePrefixes: Iterable<String>): List<ClassSource> {
    return WebContainerSource.additionalSources.filter { source -> packagePrefixes.any { source.className.startsWith(it) } }
}

fun scanAvailableClassLoaders(packagePrefixes: Iterable<String>): List<ClassSource> {
    val threadClassLoaders = Thread.getAllStackTraces().keys.mapNotNull(Thread::getContextClassLoader)
    val leafClassLoaders = threadClassLoaders
        .leaves(ClassLoader::getParent)
        .toListWith(ClassLoader.getSystemClassLoader())
    return ClassPath(packagePrefixes)
        .scan(leafClassLoaders)
        .entries
        .map { (clsName, loader) -> ClassLoaderSource(clsName.removeSuffix(".class"), loader) }

}