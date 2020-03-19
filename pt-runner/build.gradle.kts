import org.apache.tools.ant.taskdefs.condition.*
import org.jetbrains.kotlin.konan.target.*

plugins {
    java
    application
}

val target = HostManager.host.presetName

val agentJavaProject = rootProject

application {
    mainClassName = "org.springframework.boot.loader.JarLauncher"
    val (pref, ex) = when {
        Os.isFamily(Os.FAMILY_MAC) -> "lib" to "dylib"
        Os.isFamily(Os.FAMILY_UNIX) -> "lib" to "so"
        else -> "" to "dll"
    }
    val drillDistrDir = agentJavaProject.buildDir.resolve("install").resolve(target).absolutePath
    val agentPath = "${file("$drillDistrDir/${pref}drill_agent.$ex")}"
    applicationDefaultJvmArgs = listOf(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007",
        "-agentpath:$agentPath=drillInstallationDir=$drillDistrDir,adminAddress=${project.properties["adminAddress"]
            ?: "localhost:8090"},agentId=${project.properties["agentId"] ?: "Petclinic"}"
    ).apply { println(this.joinToString (separator = " ")) }

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
    (run) {
        val installTaskName = "install${target.capitalize()}Dist"
        dependsOn(agentJavaProject.tasks.named(installTaskName))
    }
}
