rootProject.name = "java-agent"

pluginManagement {
    plugins {
        id("com.epam.drill.agent.runner.app") version "0.1.2"
        id("com.github.johnrengelman.shadow") version "5.2.0"
    }
    repositories {
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
        jcenter()
    }
}

include("pt-runner")
