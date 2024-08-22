import java.net.URI
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
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
val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val ktorVersion: String by parent!!.extra
val javassistVersion: String by parent!!.extra
val transmittableThreadLocalVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra
val aesyDatasizeVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra
val nativeAgentHookEnabled: String by parent!!.extra
val macosLd64: String by parent!!.extra
val bytebuddyVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val configureNativeTarget: KotlinNativeTarget.() -> Unit = {
        compilations["test"].cinterops.create("test_stubs")
        binaries.sharedLib(nativeAgentLibName, setOf(DEBUG))
    }
    targets {
        jvm()
        linuxX64(configure = configureNativeTarget)
        mingwX64(configure = configureNativeTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        macosX64(configure = configureNativeTarget).apply {
            if(macosLd64.toBoolean()){
                binaries.all {
                    linkerOpts("-ld64")
                }
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName].compilations.forEach {
            it.defaultSourceSet.kotlin.srcDir("src/native${it.compilationName.capitalize()}/kotlin")
            it.defaultSourceSet.resources.srcDir("src/native${it.compilationName.capitalize()}/resources")
        }
        val commonMain by getting {
            kotlin.srcDir("src/commonGenerated/kotlin")
            file("src/commonGenerated/kotlin/com/epam/drill/agent").apply {
                mkdirs()
                resolve("Version.kt").writeText(
                    """
                    package com.epam.drill.agent
                    
                    internal val agentVersion = "${project.version}"
                    """.trimIndent()
                )
            }
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation(project(":logging"))
                implementation(project(":common"))
                implementation(project(":agent-config"))
                implementation(project(":agent-instrumentation"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.javassist:javassist:$javassistVersion")
                implementation("com.alibaba:transmittable-thread-local:$transmittableThreadLocalVersion")
                implementation("io.aesy:datasize:$aesyDatasizeVersion")
                implementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")
                implementation(project(":agent-transport"))
                implementation(project(":agent-instrumentation"))
                runtimeOnly(project(":test2code"))
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation(project(":jvmapi"))
                implementation(project(":knasm"))
                implementation(project(":konform"))
                if (nativeAgentHookEnabled == "true")
                    implementation(project(":interceptor-http"))
                else
                    implementation(project(":interceptor-stub"))
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
    tasks {
        val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
            it.targetName != HostManager.host.presetName
        }
        val copyNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "copyNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val copyNativeClasses: TaskProvider<Copy> = register(taskName, Copy::class) {
                group = "build"
                from("src/native${it.compilationName.capitalize()}/kotlin")
                into("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            it.compileKotlinTask.dependsOn(copyNativeClasses.get())
        }
        val cleanNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "cleanNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val cleanNativeClasses: TaskProvider<Delete> = register(taskName, Delete::class) {
                group = "build"
                delete("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            clean.get().dependsOn(cleanNativeClasses.get())
        }
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform)
            .flatMap(KotlinNativeTarget::compilations)
            .onEach(copyNativeClassesTask)
            .onEach(cleanNativeClassesTask)
        val jvmMainCompilation = kotlin.targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val relocatePackages = setOf(
            "javax.validation",
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
            "org.petitparser",
            "net.bytebuddy",
            "mu",
        )
        val runtimeJar by registering(ShadowJar::class) {
            mergeServiceFiles()
            isZip64 = true
            archiveFileName.set("drill-runtime.jar")
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
                    exclude("*.h")
                    exclude("*.def")
                }
                from("drill.properties")
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
