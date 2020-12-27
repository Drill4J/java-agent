package com.epam.drill.agent.classloading

import com.epam.drill.agent.classloading.source.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import java.io.*
import java.net.*
import kotlin.reflect.jvm.*

@Kni
actual object WebContainerSource {
    private val logger = Logging.logger(WebContainerSource::class.jvmName)

    private val webPaths = mutableSetOf<File>()

    val additionalSources: Set<ClassSource>
        get() {
            val scannedNames = mutableSetOf<String>()
            val scannedDirs = mutableSetOf<File>()
            val scannedClasses = mutableSetOf<ClassSource>()

            fun predicate(name: String): Boolean = name !in scannedNames

            fun handler(source: ClassSource) {
                scannedNames.add(source.className)
                scannedClasses.add(source)
            }

            webPaths.forEach { deployedApp ->
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
            }
            return scannedClasses
        }

    fun fillWebAppSource(warPath: String?, warResource: URL?) {
        if (warPath == null || warResource == null) {
            logger.warn { "Can't find web app sources. warPath='${warPath}', warResource='${warResource}'" }
            return
        }
        logger.warn { "Process web app: warPath='${warPath}', warResource='${warResource}'" }
        val deployedApp = File(warPath)
        webPaths += deployedApp
        webAppStarted(File(warResource.path).name)
    }

    actual external fun webAppStarted(appPath: String)
}
