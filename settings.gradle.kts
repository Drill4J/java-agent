rootProject.name = "java-agent"

val scriptUrl: String by extra
apply(from = "$scriptUrl/maven-repo.settings.gradle.kts")

pluginManagement {
    val kotlinVersion: String by extra
    val agentRunnerPluginVersion: String by extra
    val shadowPluginVersion: String by extra
    val kniVersion: String by extra
    val licenseVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.epam.drill.agent.runner.app") version agentRunnerPluginVersion
        id("com.epam.drill.gradle.plugin.kni") version kniVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("com.github.hierynomus.license") version licenseVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://oss.jfrog.org/oss-release-local")
        maven("https://drill4j.jfrog.io/artifactory/drill")
    }
}

include("bootstrap")
//include("pt-runner")
