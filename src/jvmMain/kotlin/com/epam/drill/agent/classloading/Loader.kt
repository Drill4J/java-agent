package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.agent.classloading.source.*

fun scanResourceMap(packagePrefixes: Iterable<String>): Set<ClassSource> = packagePrefixes.run {
    scanAvailableClassLoaders().apply { addAll(scanExternalSources()) }
}

fun Iterable<String>.scanExternalSources(): List<ClassSource> = WebContainerSource.additionalSources.filter {
    it.className.matches(this)
}

fun Iterable<String>.scanAvailableClassLoaders(): MutableSet<ClassSource> {
    val threadClassLoaders = Thread.getAllStackTraces().keys.mapNotNull(Thread::getContextClassLoader)
    val leafClassLoaders = threadClassLoaders
        .leaves(ClassLoader::getParent)
        .toListWith(ClassLoader.getSystemClassLoader())
    return ClassPath(this).scan(leafClassLoaders)
}
