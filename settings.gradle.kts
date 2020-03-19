rootProject.name = "java-agent"

pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "5.2.0"
    }
}

include("pt-runner")
