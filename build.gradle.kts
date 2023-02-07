@Suppress("RemoveRedundantBackticks")
plugins {
    `java-gradle-plugin`
    `maven-publish`
    `distribution`
    `java`
    `application`
    kotlin("jvm").apply(false)
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").apply(false)
    id("io.github.gradle-nexus.publish-plugin")
    id("com.github.hierynomus.license").apply(false)
    id("com.github.johnrengelman.shadow").apply(false)
    id("com.epam.drill.gradle.plugin.kni").apply(false)
    id("com.epam.drill.agent.runner.app").apply(false)
}

group = "com.epam.drill"

repositories {
    mavenLocal()
    mavenCentral()
}
