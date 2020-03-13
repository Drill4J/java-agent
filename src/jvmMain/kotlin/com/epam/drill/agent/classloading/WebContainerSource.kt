package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*
import java.io.*
import java.util.jar.*

private const val classSuffix = ".class"

object WebContainerSource {

    val additionalSources = mutableSetOf<ClassSource>()

    fun fillWebAppSource(cl: String) {
        val deployedApp = File(cl)
        val webInfDir = deployedApp.resolve("WEB-INF")
        val classesDir = webInfDir.resolve("classes")
        val jarsDir = webInfDir.resolve("lib")
        val jarSources = jarsDir.walkTopDown().filter { it.extension == "jar" }.flatMap { file ->
            JarFile(file).use { jar ->
                jar.entries()
                    .toList()
                    .filter { it.name.endsWith(classSuffix) }
                    .map { JarSource(it.name.removeSuffix(classSuffix).replace(File.pathSeparator, "/"), file) }
            }.asSequence()
        }
        val classSources = classesDir.walkTopDown().filter { it.extension == "class" }.map {
            FileSource(it.toRelativeString(classesDir).removeSuffix(classSuffix).replace(File.pathSeparator, "/"), it)
        }
        additionalSources.addAll(jarSources + classSources)
        webAppStarted(deployedApp.absolutePath)

    }

    private external fun webAppStarted(appPath: String)
}