package com.epam.drill.agent.classloading

import java.net.*
import java.util.*

internal val String.isTopLevelClass get() = !contains("$") && endsWith(".class")

internal fun String.isAllowedFor(packageNames: Iterable<String>) = toResourceName().run {
    packageNames.any { packageName -> startsWith(packageName) }
}

internal fun String.toResourceName() = removeSuffix(".class")
    .removePrefix("BOOT-INF/classes/") //fix from Spring Boot Executable jar
    .replace(".", "/")

internal fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)

internal fun ClassLoader.url(resourceName: String): URL {
    return getResource(resourceName) ?: throw NoSuchElementException(resourceName)
}

internal fun Map<String, ClassLoader>.excludePackages(prefix: String) = run {
    filterKeys { classPath -> !classPath.startsWith(prefix) }
}
