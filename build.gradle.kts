import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.konan.target.*
import java.net.*
import java.nio.file.*
import java.util.jar.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("com.epam.drill.gradle.plugin.kni")
    id("com.github.hierynomus.license")
    distribution
    `maven-publish`
}

val scriptUrl: String by extra

val kxSerializationVersion: String by extra
val kxCoroutinesVersion: String by extra
val uuidVersion: String by extra

val drillJvmApiLibVersion: String by extra
val drillApiVersion: String by extra
val drillAgentCoreVersion: String by extra
val drillLogger: String by extra
val knasmVersion: String by extra
val kniVersion: String by extra
val ktorUtilVersion: String by extra
val ttlVersion: String by extra

allprojects {
    apply(from = rootProject.uri("$scriptUrl/git-version.gradle.kts"))

    repositories {
        mavenLocal()
        mavenCentral()
        apply(from = "$scriptUrl/maven-repo.gradle.kts")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(
        file("src/generated/kotlin").apply {
            mkdirs()
            resolve("Version.kt").writeText(
                "package com.epam.drill.agent internal val agentVersion = \"${project.version}\""
            )
        }
    )
}

val libName = "drill-agent"
val nativeTargets = mutableSetOf<KotlinNativeTarget>()
val currentPlatformName = HostManager.host.presetName

kotlin {
    targets {
        nativeTargets.addAll(
            sequenceOf(
                mingwX64 {
                    binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") }
                },
                linuxX64(),
                macosX64()
            )
        )
        nativeTargets.forEach { target ->
            //TODO EPMDJ-8696 remove
            if (currentPlatformName == target.name) {
                target.compilations["main"].setCommonSources("src/commonNative")
                // TODO Resolve agent dependency in tests (PS it doesn't work before)
                // target.compilations["test"].setCommonSources("src/nativeAgentTest")
            }
            target.binaries { sharedLib(libName, setOf(DEBUG)) }
            target.compilations["test"].cinterops.create("testStubs")
        }
        jvm {
            compilations["main"].defaultSourceSet {
                dependencies {
                    implementation(kotlin("reflect"))
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kxSerializationVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kxSerializationVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kxCoroutinesVersion")
                    implementation("com.epam.drill:common:$drillApiVersion")
                    implementation("com.epam.drill.logger:logger:$drillLogger") {
                        //TODO EPMDJ-8703 exclude in logger
                        exclude("org.slf4j")
                    }
                    implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                    implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")
                    implementation("com.epam.drill.kni:runtime:$kniVersion")
                    implementation("com.epam.drill.knasm:knasm:$knasmVersion")
                    implementation("com.alibaba:transmittable-thread-local:$ttlVersion")
                }
            }
            compilations["test"].defaultSourceSet {
                dependencies {
                    implementation(kotlin("test-junit"))
                    implementation("org.junit.jupiter:junit-jupiter:5.5.2")
                }
            }
        }
    }

    sourceSets {
        listOf(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.serialization.InternalSerializationApi",
            "io.ktor.utils.io.core.ExperimentalIoApi"
        ).let { annotations ->
            all { annotations.forEach(languageSettings::optIn) }
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kxSerializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }
        //TODO EPMDJ-8696 Rename to commonNative
        val commonNativeDependenciesOnly by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kxSerializationVersion")
                implementation("com.epam.drill:jvmapi:$drillJvmApiLibVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                implementation("com.epam.drill:common:$drillApiVersion")
                implementation("com.epam.drill.logger:logger:$drillLogger")
                implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")
                implementation("com.epam.drill.knasm:knasm:$knasmVersion")
                implementation("com.epam.drill.kni:runtime:$kniVersion")
                implementation("io.ktor:ktor-utils:$ktorUtilVersion")
            }

        }

        val linuxX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }
        val mingwX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }
        val macosX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }

        kni {
            jvmTargets = sequenceOf(jvm())
            additionalJavaClasses = sequenceOf()
            jvmtiAgentObjectPath = "com.epam.drill.core.Agent"
            nativeCrossCompileTarget = nativeTargets.asSequence()
        }
    }
}

val runtimeJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    mergeServiceFiles()
    isZip64 = true
    archiveFileName.set("drillRuntime.jar")
    val main by kotlin.jvm().compilations
    from(
        provider { main.output },
        provider { main.runtimeDependencyFiles }
    )
    relocate("kotlin", "kruntime")
    doLast {
        val jarFilePath = Paths.get("$buildDir/libs", archiveFileName.get())
        val zipDisk = URI.create("jar:${jarFilePath.toUri()}")
        val zipProperties = mutableMapOf("create" to "false")
        FileSystems.newFileSystem(zipDisk, zipProperties).use { fileSystem ->
            val manifestPath = fileSystem.getPath(JarFile.MANIFEST_NAME)
            Files.delete(manifestPath)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks {
    val generateNativeClasses by getting {}
    //TODO EPMDJ-8696 remove copy
    val otherTargets = nativeTargets.filter { it.name != currentPlatformName }
    val copy = otherTargets.map {
        register<Copy>("copy for ${it.name}") {
            from(file("src/commonNative/kotlin"))
            into(file("src/${it.name}Main/kotlin/gen"))
        }
    }
    val copyCommon by registering(DefaultTask::class) {
        group = "build"
        copy.forEach { dependsOn(it) }
    }

    withType<KotlinNativeCompile> {
        dependsOn(copyCommon)
        dependsOn(generateNativeClasses)
    }
    val cleanExtraData by registering(Delete::class) {
        group = "build"
        otherTargets.forEach {
            val path = "src/${it.name}Main/kotlin/"
            delete(file("${path}kni"), file("${path}gen"))
        }
    }

    clean {
        dependsOn(cleanExtraData)
    }
}

afterEvaluate {
    val availableTargets =
        kotlin.targets.filterIsInstance<KotlinNativeTarget>().filter { HostManager().isEnabled(it.konanTarget) }

    distributions {
        availableTargets.forEach {
            val name = it.name
            create(name) {
                distributionBaseName.set(name)
                contents {
                    from(runtimeJar)
                    from(tasks.named("link${libName.capitalize()}DebugShared${name.capitalize()}")) {
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    }
                }
            }
        }
    }
    publishing {
        publications {
            availableTargets.forEach {
                create<MavenPublication>("${it.name}Zip") {
                    artifactId = "$libName-${it.name}"
                    artifact(tasks["${it.name}DistZip"])
                }
            }
        }
    }
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)

//TODO EPMDJ-8696 remove
fun KotlinNativeCompilation.setCommonSources(modulePath: String) {
    defaultSourceSet {
        kotlin.srcDir(file("${modulePath}/kotlin"))
        resources.setSrcDirs(listOf("${modulePath}/resources"))
    }
}
