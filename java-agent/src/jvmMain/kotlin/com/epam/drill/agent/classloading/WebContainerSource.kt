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

            fun predicate(source: ClassSource): Boolean = source.className !in scannedNames

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
