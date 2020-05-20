rootProject.name = "java-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val drillGradlePluginVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.epam.drill.cross-compilation") version drillGradlePluginVersion
        id("com.epam.drill.version.plugin") version drillGradlePluginVersion
        id("com.epam.drill.agent.runner.app") version "0.1.2"
        id("com.github.johnrengelman.shadow") version "5.2.0"
    }
    repositories {
        gradlePluginPortal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
    }
}

include("pt-runner")
