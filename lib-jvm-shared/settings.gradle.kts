rootProject.name = "lib-jvm-shared"

pluginManagement {
    val kotlinVersion: String by extra
    val kotlinxBenchmarkVersion: String by extra
    val atomicfuVersion: String by extra
    val licenseVersion: String by extra
    val publishVersion: String by extra
    val protobufVersion: String by extra
    val shadowPluginVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("kotlinx-atomicfu") version atomicfuVersion
        id("org.jetbrains.kotlinx.benchmark") version kotlinxBenchmarkVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("io.github.gradle-nexus.publish-plugin") version publishVersion
        id("com.google.protobuf") version protobufVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy.eachPlugin {
        if(requested.id.id == "kotlinx-atomicfu") useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${target.version}")
    }
}

include("jvmapi")
include("logging")
include("common")
// FYI: Interceptor not patched for macOS ARM64 architecture.
//include("interceptor-hook")
//include("interceptor-http")
include("interceptor-stub")
include("agent-config")
include("agent-transport")
include("agent-instrumentation")
include("ktor-swagger")
include("ktor-swagger-sample")
include("admin-analytics")
include("test2code-common")
include("konform")
include("transmittable-thread-local")
