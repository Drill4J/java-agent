rootProject.name = "java-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val grgitVersion: String by extra
    val shadowPluginVersion: String by extra
    val publishVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.ajoberstar.grgit") version grgitVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("io.github.gradle-nexus.publish-plugin") version publishVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file("lib-jvm-shared/$it")
}

includeSharedLib("kni-runtime")
includeSharedLib("kni-plugin")
includeSharedLib("jvmapi")
includeSharedLib("logger-api")
includeSharedLib("logger-test-agent")
includeSharedLib("logger")
includeSharedLib("common")
includeSharedLib("knasm")
includeSharedLib("drill-hook")
includeSharedLib("http-clients-instrumentation")
includeSharedLib("transport")
includeSharedLib("interceptor-http")
includeSharedLib("plugin-api-agent")
includeSharedLib("agent")
includeSharedLib("agent-runner-common")
includeSharedLib("agent-runner-gradle")
include("java-agent")
include("bootstrap")
include("pt-runner")
