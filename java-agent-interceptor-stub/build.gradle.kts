import java.net.URI
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
    targets {
        linuxX64()
        mingwX64()
        macosX64()
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
