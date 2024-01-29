import java.net.URI
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
val aesyDatasizeVersion: String by parent!!.extra

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
        jvm()
        linuxX64(configure = configureNativeTarget)
        mingwX64(configure = configureNativeTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        macosX64(configure = configureNativeTarget)
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
        }
        currentPlatformTarget().compilations["test"].defaultSourceSet {
            kotlin.srcDir("src/nativeTest/kotlin")
            resources.srcDir("src/nativeTest/resources")
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting {
            kotlin.srcDir("src/commonGenerated/kotlin")
            file("src/commonGenerated/kotlin/com/epam/drill/agent").apply {
                mkdirs()
                resolve("Version.kt").writeText("""
                    package com.epam.drill.agent
                    
                    internal val agentVersion = "${project.version}"
                    internal val nativeAgentLibName = "$nativeAgentLibName"
                """.trimIndent())
            }
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation(project(":http-clients-instrumentation"))
                implementation(project(":logging"))
                implementation(project(":common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.javassist:javassist:$javassistVersion")
                implementation("com.alibaba:transmittable-thread-local:$transmittableThreadLocalVersion")
                implementation("io.aesy:datasize:$aesyDatasizeVersion")
                implementation(project(":agent"))
                implementation(project(":http-clients-instrumentation"))
                implementation(project(":test2code"))
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation(project(":agent"))
                implementation(project(":jvmapi"))
                implementation(project(":knasm"))
                implementation(project(":konform"))
            }
        }
        val configureNativeTestDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
        val linuxX64Test by getting(configuration = configureNativeTestDependencies)
        val mingwX64Test by getting(configuration = configureNativeTestDependencies)
        val macosX64Test by getting(configuration = configureNativeTestDependencies)
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
    val copyNativeTestClassesForTarget: TaskContainer.(KotlinNativeTarget) -> Task = {
        val copyNativeTestClasses:TaskProvider<Copy> = register("copyNativeTestClasses${it.targetName.capitalize()}", Copy::class) {
            group = "build"
            from("src/nativeTest/kotlin")
            into("src/${it.targetName}Test/kotlin/gen")
        }
        copyNativeTestClasses.get()
    }
    val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
        it.targetName != HostManager.host.presetName
    }
    tasks {
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            val copyNativeTestClasses = copyNativeTestClassesForTarget(it)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
            it.compilations["test"].compileKotlinTask.dependsOn(copyNativeTestClasses)
        }
        val jvmMainCompilation = kotlin.targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val relocatePackages = setOf(
            "javax.validation",
            "javax.websocket",
            "javassist",
            "ch.qos.logback",
            "io.aesy.datasize",
            "com.alibaba",
            "org.slf4j",
            "org.jacoco",
            "org.objectweb.asm",
            "org.apache.bcel",
            "org.apache.commons",
            "org.apache.hc",
            "org.eclipse.jetty",
            "org.intellij.lang.annotations",
            "org.jetbrains.annotations",
            "org.petitparser"
        )
        val runtimeJar by registering(ShadowJar::class) {
            mergeServiceFiles()
            isZip64 = true
            archiveFileName.set("drillRuntime.jar")
            from(jvmMainCompilation.output, jvmMainCompilation.runtimeDependencyFiles)
            relocate("kotlin", "kruntime")
            relocate("kotlinx", "kruntimex")
            relocatePackages.forEach {
                relocate(it, "${project.group}.shadow.$it")
            }
            dependencies {
                exclude("/META-INF/services/javax.servlet.ServletContainerInitializer")
                exclude("/module-info.class", "/about.html")
                exclude("/ch/qos/logback/classic/servlet/*")
            }
        }
        runtimeJar.get().dependsOn(jvmMainCompilation.compileKotlinTask)
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            targets.withType<KotlinNativeTarget> {
                delete("src/${name}Main/kotlin/gen")
                delete("src/${name}Test/kotlin/gen")
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
                from("drill.properties")
                from("temporary.jks")
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
            exclude("**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/commonGenerated")
        }
    }
}
