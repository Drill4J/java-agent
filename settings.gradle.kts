rootProject.name = "java-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val atomicfuVersion: String by extra
    val licenseVersion: String by extra
    val grgitVersion: String by extra
    val shadowPluginVersion: String by extra
    val publishVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("kotlinx-atomicfu") version atomicfuVersion
        id("org.ajoberstar.grgit") version grgitVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("io.github.gradle-nexus.publish-plugin") version publishVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy.eachPlugin {
        if(requested.id.id == "kotlinx-atomicfu") useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${target.version}")
    }
}

val sharedLibsLocalPath: String by extra
val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file(sharedLibsLocalPath).resolve(it)
}

includeSharedLib("logging")
includeSharedLib("common")
includeSharedLib("agent-config")
includeSharedLib("agent-transport")
includeSharedLib("agent-instrumentation")
includeSharedLib("jvmapi")
includeSharedLib("knasm")
includeSharedLib("konform")
// FYI: Interceptor not patched for macOS ARM64 architecture.
//includeSharedLib("interceptor-hook")
//includeSharedLib("interceptor-http")
includeSharedLib("interceptor-stub")
includeSharedLib("dsm-annotations")
includeSharedLib("test2code-common")
include("test2code-jacoco")
include("test2code")
include("java-agent")
