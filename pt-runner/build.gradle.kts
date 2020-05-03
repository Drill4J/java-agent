import org.jetbrains.kotlin.konan.target.*

plugins {
    java
    application
    id("com.epam.drill.agent.runner.app") version "0.1.2"
}

val target = HostManager.host.presetName

val agentJavaProject = rootProject

application {
    mainClassName = "org.springframework.boot.loader.JarLauncher"
}

drill {
    val (prefix, suffix) = HostManager.host.family.run {
        dynamicPrefix to dynamicSuffix
    }
    val drillDistrDir = agentJavaProject.buildDir.resolve("install").resolve(target)
    val localAgentPath = file(drillDistrDir).resolve("${prefix}drill_agent.$suffix")
    agentId = project.properties["agentId"]?.toString() ?: "Petclinic"
    agentPath = localAgentPath
    runtimePath = drillDistrDir
    adminHost = "localhost"
    adminPort = 8090
    logLevel = com.epam.drill.agent.runner.LogLevels.TRACE
}

repositories {
    mavenLocal()
    maven("https://dl.bintray.com/drill/drill4j")
}

dependencies {
    compileOnly("org.springframework:spring-context:5.1.8.RELEASE")
    implementation("org.springframework.samples:spring-petclinic:2.1.0") { isTransitive = false }
}

tasks {
    (run) {
        val installTaskName = "install${target.capitalize()}Dist"
        dependsOn(agentJavaProject.tasks.named(installTaskName))
    }
}
