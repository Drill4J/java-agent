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
project(":kni-runtime").projectDir = file("../shared-libs/kni-runtime")
project(":kni-plugin").projectDir = file("../shared-libs/kni-plugin")
project(":agent-runner-common").projectDir = file("../shared-libs/agent-runner-common")
project(":agent-runner-gradle").projectDir = file("../shared-libs/agent-runner-gradle")
