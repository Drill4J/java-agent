import java.util.Properties
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
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
    jvm()
    mingwX64()
    linuxX64()
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
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.5.2")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    tasks {
        val jvmTest by getting(KotlinJvmTest::class) {
            useJUnitPlatform()
        }
    }
    tasks.withType<JavaCompile> {
        options.compilerArgs = (options.compilerArgs + "-Xlint:none").toMutableList()
    }
}
