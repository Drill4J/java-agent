package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*
import com.epam.drill.logging.*
import java.io.*
import java.net.*

object WebContainerSource {
    private val scannedNames = mutableSetOf<String>()

    private val scannedDirs = mutableSetOf<File>()

    private val scannedClasses = mutableSetOf<ClassSource>()

    val additionalSources: Set<ClassSource> = scannedClasses

    fun fillWebAppSource(warPath: String?, warResource: URL?) {
        if (warPath == null || warResource == null) {
            log(Level.INFO) { "Can't find web app sources. warPath='${warPath}', warResource='${warResource}'" }
            return
        }
        log(Level.INFO) { "Process web app: warPath='${warPath}', warResource='${warResource}'" }
        val deployedApp = File(warPath)
        deployedApp.parentFile
            .listFiles { pathname -> pathname?.name?.contains(".jar") ?: false }
            ?.filter { !scannedDirs.contains(it) }?.forEach {
                it.walkTopDown()
                    .filter { it.isFile && it.extension == "jar" }
                    .forEach { file -> file.useJarInputStream { it.scan(::predicate, ::handler) } }
            }
        val webInfDir = deployedApp.resolve("WEB-INF")
        webInfDir.resolve("classes").scan(::predicate, ::handler)
        webInfDir.resolve("lib").walkTopDown()
            .filter { it.isFile && it.extension == "jar" }
            .forEach { file -> file.useJarInputStream { it.scan(::predicate, ::handler) } }
        webAppStarted(File(warResource.path).name)
    }

    private fun predicate(name: String): Boolean = name !in scannedNames

    private fun handler(source: ClassSource) {
        scannedNames.add(source.className)
        scannedClasses.add(source)
    }

    private external fun webAppStarted(appPath: String)
}
