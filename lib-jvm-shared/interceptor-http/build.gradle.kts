import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val logbackVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra

repositories {
    mavenCentral()
}

kotlin {
    val configureIntTestTarget: KotlinTarget.() -> Unit = {
        compilations.create("intTest").associateWith(compilations["main"])
        (this as? KotlinNativeTarget)?.binaries?.sharedLib(nativeAgentLibName, setOf(DEBUG)) {
            compilation = compilations["intTest"]
        }
    }
    targets {
        jvm(configure = configureIntTestTarget)
        linuxX64(configure = configureIntTestTarget)
        mingwX64(configure = configureIntTestTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        targets.withType<KotlinNativeTarget>().findByName(HostManager.host.presetName)?.compilations?.forEach {
            it.defaultSourceSet.kotlin.srcDir("src/native${it.name.capitalize()}/kotlin")
            it.defaultSourceSet.resources.srcDir("src/native${it.name.capitalize()}/resources")
        }
        val jvmIntTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(project(":common"))
                implementation("org.apache.tomcat.embed:tomcat-embed-core:10.0.27")
                implementation("org.eclipse.jetty:jetty-server:9.4.26.v20200117")
                implementation("io.undertow:undertow-core:2.0.29.Final")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
            }
        }
        val configureNativeMainDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                api("io.github.microutils:kotlin-logging:$microutilsLoggingVersion")
                implementation(project(":common"))
                implementation(project(":interceptor-hook"))
            }
        }
        val configureNativeIntTestDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation(project(":jvmapi"))
            }
        }
        val linuxX64Main by getting(configuration = configureNativeMainDependencies)
        val mingwX64Main by getting(configuration = configureNativeMainDependencies)
        val linuxX64IntTest by getting(configuration = configureNativeIntTestDependencies)
        val mingwX64IntTest by getting(configuration = configureNativeIntTestDependencies)
    }
    tasks {
        val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
            it.targetName != HostManager.host.presetName
        }
        val copyNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "copyNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val copyNativeClasses: TaskProvider<Copy> = register(taskName, Copy::class) {
                group = "build"
                from("src/native${it.compilationName.capitalize()}/kotlin")
                into("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            it.compileKotlinTask.dependsOn(copyNativeClasses.get())
        }
        val cleanNativeClassesTask: (KotlinCompilation<*>) -> Unit = {
            val taskName = "cleanNativeClasses${it.target.targetName.capitalize()}${it.compilationName.capitalize()}"
            val cleanNativeClasses: TaskProvider<Delete> = register(taskName, Delete::class) {
                group = "build"
                delete("src/${it.target.targetName}${it.compilationName.capitalize()}/kotlin/gen")
            }
            clean.get().dependsOn(cleanNativeClasses.get())
        }
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform)
            .flatMap(KotlinNativeTarget::compilations)
            .onEach(copyNativeClassesTask)
            .onEach(cleanNativeClassesTask)
        register("integrationTest", Test::class) {
            val intTestAgentLib = targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
                .binaries.getSharedLib(nativeAgentLibName, NativeBuildType.DEBUG)
            val intTestCompilation = targets.withType<KotlinJvmTarget>()["jvm"].compilations["intTest"]
            description = "Runs the integration tests using simple native agent"
            group = "verification"
            testClassesDirs = intTestCompilation.output.classesDirs
            classpath = intTestCompilation.runtimeDependencyFiles + intTestCompilation.output.allOutputs
            jvmArgs = listOf("-agentpath:${intTestAgentLib.outputFile.path}")
            dependsOn(intTestAgentLib.linkTask)
        }
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
}
