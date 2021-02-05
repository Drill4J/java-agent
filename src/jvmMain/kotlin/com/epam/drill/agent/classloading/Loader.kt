/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
