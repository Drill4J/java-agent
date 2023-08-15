import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `distribution`
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill"
version = rootProject.version

val kotlinxSerializationVersion: String by parent!!.extra
val ktorVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val configureNativeTarget: KotlinNativeTarget.() -> Unit = {
        binaries.sharedLib(nativeAgentLibName, setOf(DEBUG))
    }
    val currentPlatformTarget: KotlinMultiplatformExtension.() -> KotlinNativeTarget = {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
    }
    targets {
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
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation(project(":logging"))
                implementation(project(":common"))
                implementation(project(":jvmapi"))
                implementation(project(":knasm"))
                implementation(project(":agent"))
            }
        }
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
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
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
        }
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            targets.withType<KotlinNativeTarget> {
                delete("src/${name}Main/kotlin/gen")
            }
        }
        clean.dependsOn(cleanGeneratedClasses)
    }
}

distributions {
    val filterEnabledNativeTargets: (KotlinNativeTarget) -> Boolean = {
        HostManager().isEnabled(it.konanTarget)
    }
    val enabledNativeTargets = kotlin.targets.withType<KotlinNativeTarget>().filter(filterEnabledNativeTargets)
    enabledNativeTargets.forEach {
        val nativeAgentLinkTask = tasks["link${nativeAgentLibName.capitalize()}DebugShared${it.targetName.capitalize()}"]
        create(it.targetName) {
            distributionBaseName.set(it.targetName)
            contents {
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
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
}
