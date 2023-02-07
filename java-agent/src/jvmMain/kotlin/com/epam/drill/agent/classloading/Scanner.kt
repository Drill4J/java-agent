/**
 * Copyright 2020 - 2022 EPAM Systems
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

import com.epam.drill.*
import com.epam.drill.agent.classloading.source.*
import org.objectweb.asm.*
import java.io.*
import java.util.jar.*

internal fun File.useJarInputStream(block: (JarInputStream) -> Unit) {
    JarInputStream(inputStream().buffered(256 * 1024)).use(block)
}

internal fun JarInputStream.scan(
    predicate: (ClassSource) -> Boolean,
    handler: (ClassSource) -> Unit
): Unit = forEachFile { entry: JarEntry ->
    val name = entry.name
    when (name.substringAfterLast('.')) {
        "class" -> name.toClassName().removePrefix(SPRING_BOOT_PREFIX).toClassSource().takeIf {
            it.isAllowed() && predicate(it)
        }?.let {
            val bytes = readBytes()
            it.copy(bytes = bytes, superName = ClassReader(bytes).superName ?: "")
        }?.takeIf(predicate)?.let(handler)
        "jar" -> JarInputStream(ByteArrayInputStream(readBytes())).scan(predicate, handler)
    }
}

internal fun File.scan(
    predicate: (ClassSource) -> Boolean,
    handler: (ClassSource) -> Unit
) = walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { f ->
    val source = f.toRelativeString(this)
        .removePrefix(SPRING_BOOT_PREFIX)
        .replace(File.separatorChar, '/')
        .toClassName()
        .toClassSource()
    if (source.isAllowed() && predicate(source)) {
        val bytes = f.readBytes()
        source.copy(bytes = bytes, superName = ClassReader(bytes).superName ?: "")
            .takeIf(predicate)
            ?.let(handler)
    }
}

private tailrec fun JarInputStream.forEachFile(block: JarInputStream.(JarEntry) -> Unit) {
    val entry = nextJarEntry ?: return
    if (!entry.isDirectory) {
        block(entry)
    }
    forEachFile(block)
}

private fun String.toClassName() = removeSuffix(".class")

private fun ClassSource.isAllowed(): Boolean = run {
    '$' !in className && !className.startsWith(DRILL_PACKAGE) && !className.startsWith("com/alibaba/ttl")
}
