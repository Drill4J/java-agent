rootProject.name = "java-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val drillGradlePluginVersion: String by extra
    val agentRunnerPluginVersion: String by extra
    val shadowPluginVersion: String by extra
    val kniVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.epam.drill.cross-compilation") version drillGradlePluginVersion
        id("com.epam.drill.agent.runner.app") version agentRunnerPluginVersion
        id("com.epam.drill.gradle.plugin.kni") version kniVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
    }
}

include("pt-runner")
