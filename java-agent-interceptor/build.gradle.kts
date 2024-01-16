import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill"
version = rootProject.version

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val currentPlatformTarget: KotlinMultiplatformExtension.() -> KotlinNativeTarget = {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
    }
    targets {
        linuxX64()
        mingwX64()
        macosX64()
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation(project(":logging"))
                implementation(project(":common"))
                implementation(project(":drill-hook"))
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
