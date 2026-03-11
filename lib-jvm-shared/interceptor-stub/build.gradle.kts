import java.net.URI
import java.util.Properties
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}
val macosLd64: String by parent!!.extra

repositories {
    mavenCentral()
}

kotlin {
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
        val commonMain by getting
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":common"))
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        val mingwX64Main by getting{
            dependsOn(nativeMain)
        }
        val macosX64Main by getting{
            dependsOn(nativeMain)
        }
        val macosArm64Main by getting{
            dependsOn(nativeMain)
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
