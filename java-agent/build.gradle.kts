import java.net.URI
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.jar.JarFile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `maven-publish`
    `distribution`
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
    id("com.epam.drill.gradle.plugin.kni")
}

group = "com.epam.drill"
version = rootProject.version

val kotlinxSerializationVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val ktorVersion: String by parent!!.extra
val javassistVersion: String by parent!!.extra
val transmittableThreadLocalVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val configureNativeTarget: KotlinNativeTarget.() -> Unit = {
        compilations["test"].cinterops.create("testStubs")
        binaries.sharedLib(nativeAgentLibName, setOf(DEBUG))
    }
    val currentPlatformTarget: KotlinMultiplatformExtension.() -> KotlinNativeTarget = {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
    }
    targets {
        val jvm = jvm()
        val linuxX64 = linuxX64(configure = configureNativeTarget)
        val mingwX64 = mingwX64(configure = configureNativeTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        val macosX64 = macosX64(configure = configureNativeTarget)
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
        }
        kni {
            jvmTargets = sequenceOf(jvm)
            jvmtiAgentObjectPath = "com.epam.drill.core.Agent"
            nativeCrossCompileTarget = sequenceOf(linuxX64, mingwX64, macosX64)
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting {
            kotlin.srcDir("src/commonGenerated/kotlin")
            file("src/commonGenerated/kotlin/com/epam/drill/agent").apply {
                mkdirs()
                resolve("Version.kt").writeText("package com.epam.drill.agent\n\ninternal val agentVersion = \"${project.version}\"")
            }
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation(project(":http-clients-instrumentation"))
                implementation(project(":logging"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation(project(":common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                implementation("org.javassist:javassist:$javassistVersion")
                implementation("com.alibaba:transmittable-thread-local:$transmittableThreadLocalVersion")
                implementation(project(":kni-runtime"))
                implementation(project(":common"))
                implementation(project(":knasm"))
                implementation(project(":plugin-api-agent"))
                implementation(project(":http-clients-instrumentation"))
                implementation(project(":agent"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.junit.jupiter:junit-jupiter:5.5.2")
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation(project(":kni-runtime"))
                implementation(project(":common"))
                implementation(project(":jvmapi"))
                implementation(project(":knasm"))
                implementation(project(":plugin-api-agent"))
                implementation(project(":agent"))
            }
        }
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
        mingwX64Main.dependencies {
            implementation(project(":logging-native"))
        }
        macosX64Main.dependencies {
            implementation(project(":logging-native"))
        }
    }
    val copyNativeClassesForTarget: TaskContainer.(KotlinNativeTarget) -> Task = {
        val copyNativeClasses:TaskProvider<Copy> = register("copyNativeClasses${it.targetName.capitalize()}", Copy::class) {
            group = "build"
            from("src/nativeMain/kotlin")
            into("src/${it.targetName}Main/kotlin/gen")
        }
        copyNativeClasses.get()
    }
    val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
        it.targetName != HostManager.host.presetName
    }
    tasks {
        val generateNativeClasses by getting
        val jvmProcessResources by getting
        jvmProcessResources.dependsOn(generateNativeClasses)
        currentPlatformTarget().compilations["main"].compileKotlinTask.dependsOn(generateNativeClasses)
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            copyNativeClasses.dependsOn(generateNativeClasses)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
        }
        val jvmMainCompilation = kotlin.targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val runtimeJar by registering(ShadowJar::class) {
            mergeServiceFiles()
            isZip64 = true
            archiveFileName.set("drillRuntime.jar")
            from(jvmMainCompilation.output, jvmMainCompilation.runtimeDependencyFiles)
            relocate("kotlin", "kruntime")
            relocate("org.objectweb.asm", "com.epam.drill.knasm")
            relocate("org.slf4j", "com.epam.drill.slf4j")
            doLast {
                val jarFileUri = Paths.get("$buildDir/libs", archiveFileName.get()).toUri()
                val zipDisk = URI.create("jar:$jarFileUri")
                val zipProperties = mutableMapOf("create" to "false")
                FileSystems.newFileSystem(zipDisk, zipProperties).use {
                    Files.delete(it.getPath(JarFile.MANIFEST_NAME))
                }
            }
        }
        runtimeJar.get().dependsOn(jvmMainCompilation.compileKotlinTask)
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            delete("src/jvmMain/resources/kni-meta-info")
            delete("src/nativeMain/kotlin/kni")
            targets.withType<KotlinNativeTarget> {
                delete("src/${name}Main/kotlin/kni")
                delete("src/${name}Main/kotlin/gen")
            }
        }
        clean.dependsOn(cleanGeneratedClasses)
        withType<KotlinNativeTest> {
            testLogging.showStandardStreams = true
        }
        withType<Copy>().getByName("jvmProcessResources") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

distributions {
    val filterEnabledNativeTargets: (KotlinNativeTarget) -> Boolean = {
        HostManager().isEnabled(it.konanTarget)
    }
    val enabledNativeTargets = kotlin.targets.withType<KotlinNativeTarget>().filter(filterEnabledNativeTargets)
    enabledNativeTargets.forEach {
        val runtimeJarTask = tasks["runtimeJar"]
        val nativeAgentLinkTask = tasks["link${nativeAgentLibName.capitalize()}DebugShared${it.targetName.capitalize()}"]
        create(it.targetName) {
            distributionBaseName.set(it.targetName)
            contents {
                from(runtimeJarTask)
                from(nativeAgentLinkTask) {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
}
