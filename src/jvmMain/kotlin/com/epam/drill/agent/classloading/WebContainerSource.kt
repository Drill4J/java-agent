package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*
import java.io.*

object WebContainerSource {

    private val scannedNames = mutableSetOf<String>()

    private val scannedClasses = mutableSetOf<ClassSource>()

    val additionalSources: Set<ClassSource> = scannedClasses

    fun fillWebAppSource(cl: String?) {
        cl?.let { path ->
            val deployedApp = File(path)
            val webInfDir = deployedApp.resolve("WEB-INF")
            webInfDir.resolve("classes").scan(::predicate, ::handler)
            webInfDir.resolve("lib").walkTopDown()
                .filter { it.isFile && it.extension == "jar" }
                .forEach { file -> file.useJarInputStream { it.scan(::predicate, ::handler) } }
            webAppStarted(deployedApp.name)
        }
    }

    private fun predicate(name: String): Boolean = name !in scannedNames

    private fun handler(source: ClassSource) {
        scannedNames.add(source.className)
        scannedClasses.add(source)
    }

    private external fun webAppStarted(appPath: String)
}
