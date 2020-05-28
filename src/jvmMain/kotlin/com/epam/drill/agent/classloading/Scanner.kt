package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*
import java.io.*
import java.util.jar.*

private val excludedPaths = listOf(
    "com/epam/drill",
    "com/alibaba/ttl"
)

internal fun String.startsWithAnyOf(prefixes: Iterable<String>): Boolean {
    return prefixes.none() || prefixes.any { startsWith(it) }
}

internal fun File.useJarInputStream(block: (JarInputStream) -> Unit) {
    JarInputStream(inputStream().buffered(256 * 1024)).use(block)
}

internal fun JarInputStream.scan(
    predicate: (String) -> Boolean,
    handler: (ClassSource) -> Unit
): Unit = forEachFile { entry ->
    val name = entry.name
    when(name.substringAfterLast('.')) {
        "class" -> name.toClassName().let {
            val prefix = "BOOT-INF/classes/" //TODO check manifest
            if (it.startsWith(prefix)) {
                it.removePrefix(prefix)
            } else it
        }.takeIf {  it.isAllowed() && predicate(it) }?.let {
            handler(ByteArrayClassSource(it, readBytes()))
        }
        "jar" -> JarInputStream(ByteArrayInputStream(readBytes())).scan(predicate, handler)
    }
}

internal fun File.scan(
    predicate: (String) -> Boolean,
    handler: (ClassSource) -> Unit
) = walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { f ->
    val name = f.toRelativeString(this).replace(File.separatorChar, '/').toClassName()
    if (name.isAllowed() && predicate(name)) {
        handler(FileSource(name, f))
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

private fun String.isAllowed() = !contains('$') && !startsWithAnyOf(excludedPaths)
