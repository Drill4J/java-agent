pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

include("kni-runtime")
include("kni-plugin")
include("agent-runner-common")
include("agent-runner-gradle")
project(":kni-runtime").projectDir = file("../lib-jvm-shared/kni-runtime")
project(":kni-plugin").projectDir = file("../lib-jvm-shared/kni-plugin")
project(":agent-runner-common").projectDir = file("../lib-jvm-shared/agent-runner-common")
project(":agent-runner-gradle").projectDir = file("../lib-jvm-shared/agent-runner-gradle")
