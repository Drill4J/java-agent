import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import java.net.URI

plugins {
    java
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent"
version = rootProject.version
description = "The missing Java std lib (simple & 0-dependency) for framework/middleware, provide an enhanced InheritableThreadLocal that transmits values between threads even using thread pooling components."

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}
val javassistVersion: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra

dependencies {
    // Optional dependencies (compileOnly)
    compileOnly("org.javassist:javassist:$javassistVersion")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.3")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Test dependencies
    testImplementation("org.javassist:javassist:$javassistVersion")
    testImplementation("org.apache.commons:commons-lang3:3.18.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    testImplementation("io.reactivex.rxjava2:rxjava:2.2.21")
    testImplementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")
    testImplementation("io.kotest:kotest-runner-junit4-jvm:5.8.0")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
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