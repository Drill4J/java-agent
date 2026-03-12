import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val ktorVersion: String by parent!!.extra
val macosLd64: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
    mingwX64()
    macosX64().apply {
        if(macosLd64.toBoolean()){
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    macosArm64().apply {
        if (macosLd64.toBoolean()) {
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(project(":konform"))
                api("io.github.microutils:kotlin-logging:${microutilsLoggingVersion}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation(project(":konform"))
            }
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }
        val mingwX64Test by getting {
            dependsOn(nativeTest)
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        val linuxX64Test by getting {
            dependsOn(nativeTest)
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Test by getting {
            dependsOn(nativeTest)
        }
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
        val macosArm64Test by getting {
            dependsOn(nativeTest)
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
