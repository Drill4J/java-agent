import org.jetbrains.kotlin.konan.target.*

plugins {
    java
    application
    id("com.epam.drill.agent.runner.app")
}

val target = HostManager.host.presetName

val agentJavaProject = rootProject

application {
    mainClass.set("org.springframework.boot.loader.JarLauncher")
}

val emulateBigApp: Boolean
    get() = extra["emulateBigApp"]?.toString()?.toBoolean() ?: false

drill {
    val (prefix, suffix) = HostManager.host.family.run {
        dynamicPrefix to dynamicSuffix
    }
    val drillDistrDir = agentJavaProject.buildDir.resolve("install").resolve(target)
    val localAgentPath = file(drillDistrDir).resolve("${prefix}drill_agent.$suffix")
    agentId = project.properties["agentId"]?.toString() ?: "Petclinic"
    agentPath = localAgentPath
    runtimePath = drillDistrDir
    buildVersion = "0.1.7"
    adminHost = "localhost"
    adminPort = 8090
    logLevel = com.epam.drill.agent.runner.LogLevels.INFO
    logFile = rootProject
        .buildDir
        .resolve("drill-${project.version}.log")
    jvmArgs = jvmArgs + "-Ddrill.http.hook.enabled=true"
    if (emulateBigApp)
        jvmArgs = jvmArgs + "-Xmx8g"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://drill4j.jfrog.io/artifactory/drill")
}


dependencies {
    compileOnly("org.springframework:spring-context:5.1.8.RELEASE")
    implementation("org.springframework.samples:spring-petclinic:2.1.0.BUILD-SNAPSHOT") { isTransitive = false }
    if (emulateBigApp)
        implementation("com.epam.drill:petclinic-big-app:1.0.0")
}

tasks {
    (run) {
        val installTaskName = "install${target.capitalize()}Dist"
        dependsOn(agentJavaProject.tasks.named(installTaskName))
    }
}
