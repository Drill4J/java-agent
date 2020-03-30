rootProject.name = "java-agent"

pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "5.2.0"
    }
    repositories {
        mavenLocal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
}

include("pt-runner")
