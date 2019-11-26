import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    java
    application
}
val isDevMode = System.getProperty("idea.active") == "true"
val target = when {
    HostManager.hostIsMingw -> "mingwX64"
    HostManager.hostIsLinux -> "linuxX64"
    else -> "macosX64"
}
application {
    mainClassName = "org.springframework.boot.loader.JarLauncher"
    val (pref, ex) = when {
        Os.isFamily(Os.FAMILY_MAC) -> Pair("lib", "dylib")
        Os.isFamily(Os.FAMILY_UNIX) -> Pair("lib", "so")
        else -> Pair("", "dll")
    }
    val drillDistrDir =
        project(":agent:java").buildDir.resolve("install").resolve(if (isDevMode) "nativeAgent" else target).absolutePath
    val agentPath = "${file("$drillDistrDir/${pref}drill_agent.$ex")}"
    applicationDefaultJvmArgs = listOf(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007",
        "-javaagent:${drillDistrDir}/drill-proxy.jar=ttl.agent.logger:STDOUT",
        "-agentpath:$agentPath=drillInstallationDir=$drillDistrDir,adminAddress=${project.properties["adminAddress"]
            ?: "localhost:8090"},agentId=${project.properties["agentId"] ?: "Petclinic"}"
    )

}
repositories {
    mavenLocal()
    maven("https://dl.bintray.com/drill/drill4j")
}

dependencies {
    compileOnly("org.springframework:spring-context:5.1.8.RELEASE")
    implementation("org.springframework.samples:spring-petclinic:2.1.0")
}
tasks {
    named("run") {
        if (isDevMode)
            dependsOn(rootProject.tasks.getByPath(":agent:java:installNativeAgentDist"))
        else
            dependsOn(rootProject.tasks.getByPath(":agent:java:install${target.capitalize()}Dist"))
    }
}