import java.net.URI
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import com.epam.drill.agent.runner.LogLevels

@Suppress("RemoveRedundantBackticks")
plugins {
    `java`
    `application`
    id("com.github.hierynomus.license")
    id("com.epam.drill.agent.runner.app")
}

group = "com.epam.drill"

val ptEmulateBigApp: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://spring-rich-c.sourceforge.net/maven2repository")
}

dependencies {
    compileOnly("org.springframework:spring-context:5.1.8.RELEASE")
    implementation("org.springframework.samples:spring-petclinic:2.5.1") { isTransitive = false }
    if(ptEmulateBigApp.toBoolean()) implementation("com.epam.drill:petclinic-big-app:1.0.0")
}

application {
    mainClass.set("org.springframework.boot.loader.JarLauncher")
}

tasks {
    val installAgentTask = project(":java-agent").tasks["install${HostManager.host.presetName.capitalize()}Dist"]
    run.get().dependsOn(installAgentTask)
}

drill {
    val (prefix, suffix) = HostManager.host.family.run { dynamicPrefix to dynamicSuffix }
    val drillInstallPath = project(":java-agent").buildDir.resolve("install").resolve(HostManager.host.presetName)
    val drillAgentFileName = "$prefix${nativeAgentLibName.replace("-", "_")}.$suffix"
    val drillAgentPath = file(drillInstallPath).resolve(drillAgentFileName)
    agentId = properties["agentId"]?.toString() ?: "Petclinic"
    agentPath = drillAgentPath
    runtimePath = drillInstallPath
    buildVersion = version
    adminHost = "localhost"
    adminPort = 8090
    logLevel = LogLevels.INFO
    logFile = buildDir.resolve("drill-${version}.log")
    jvmArgs += "-Ddrill.http.hook.enabled=true"
    if(ptEmulateBigApp.toBoolean()) jvmArgs += "-Xmx8g"
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
}
