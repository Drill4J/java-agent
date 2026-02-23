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
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("multiplatform")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent"
version = Properties().run {
    projectDir.parentFile.resolve("versions.properties").reader().use { load(it) }
    getProperty("version.$name") ?: Project.DEFAULT_VERSION
}

val javassistVersion: String by parent!!.extra
val transmittableThreadLocalVersion: String by parent!!.extra
val bytebuddyVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra
val macosLd64: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val kotlinxCollectionsVersion: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra
val atomicfuVersion: String by parent!!.extra
val ktorVersion: String by parent!!.extra

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
    jvm(configure = configureIntTestTarget)
    linuxX64(configure = configureIntTestTarget)
    macosX64(configure = configureIntTestTarget).apply {
        if (macosLd64.toBoolean()) {
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    macosArm64(configure = configureIntTestTarget).apply {
        if (macosLd64.toBoolean()) {
            binaries.all {
                linkerOpts("-ld64")
            }
        }
    }
    mingwX64(configure = configureIntTestTarget).apply {
        binaries.all {
            linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName].compilations.forEach {
            it.defaultSourceSet.kotlin.srcDir("src/native${it.compilationName.capitalize()}/kotlin")
            it.defaultSourceSet.resources.srcDir("src/native${it.compilationName.capitalize()}/resources")
        }
        val commonMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging:$microutilsLoggingVersion")
                implementation(project(":common"))
            }
        }
        val commonIntTest by creating
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("org.javassist:javassist:$javassistVersion")
                implementation(project(":transmittable-thread-local"))
                implementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")
            }
        }
        val jvmIntTest by getting {
            dependsOn(commonIntTest)
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.apache.httpcomponents:httpclient:4.5.14")
                implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.83")
                implementation("org.apache.tomcat.embed:tomcat-embed-websocket:9.0.83")
                implementation("org.eclipse.jetty:jetty-server:9.4.26.v20200117")
                implementation("org.eclipse.jetty.websocket:javax-websocket-server-impl:9.4.26.v20200117")
                implementation("io.undertow:undertow-core:2.0.29.Final")
                implementation("io.undertow:undertow-websockets-jsr:2.0.29.Final")
                implementation("io.netty:netty-codec-http:4.1.106.Final")
                implementation("com.squareup.okhttp3:okhttp:3.12.13")
                implementation("org.simpleframework:simple-http:6.0.1")
                implementation("org.glassfish.tyrus:tyrus-client:1.20")
                implementation("org.glassfish.tyrus:tyrus-server:1.20")
                implementation("org.glassfish.tyrus:tyrus-container-grizzly-client:1.20")
                implementation("org.glassfish.tyrus:tyrus-container-grizzly-server:1.20")

                implementation("org.springframework.kafka:spring-kafka:2.9.13")
                implementation("org.springframework.kafka:spring-kafka-test:2.9.13")
                implementation("org.springframework.boot:spring-boot-starter-webflux:2.7.18")
                implementation("org.springframework.boot:spring-boot-starter-test:2.7.18")
            }
        }
        val configureNativeMainDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation(project(":jvmapi"))
            }
        }
        val configureNativeIntTestDependencies: KotlinSourceSet.() -> Unit = {
            dependsOn(commonIntTest)
            dependencies {
                implementation("io.ktor:ktor-utils:$ktorVersion")
                implementation(project(":logging"))
            }
        }
        val mingwX64Main by getting(configuration = configureNativeMainDependencies)
        val linuxX64Main by getting(configuration = configureNativeMainDependencies)
        val macosX64Main by getting(configuration = configureNativeMainDependencies)
        val macosArm64Main by getting(configuration = configureNativeMainDependencies)
        val mingwX64IntTest by getting(configuration = configureNativeIntTestDependencies)
        val linuxX64IntTest by getting(configuration = configureNativeIntTestDependencies)
        val macosX64IntTest by getting(configuration = configureNativeIntTestDependencies)
        val macosArm64IntTest by getting(configuration = configureNativeIntTestDependencies)
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
        val jvmMainCompilation = targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val jvmIntTestCompilation = targets.withType<KotlinJvmTarget>()["jvm"].compilations["intTest"]
        val runtimeJar by registering(ShadowJar::class) {
            mergeServiceFiles()
            isZip64 = true
            archiveFileName.set("drill-runtime.jar")
            from(jvmMainCompilation.runtimeDependencyFiles, jvmMainCompilation.output, jvmIntTestCompilation.output.classesDirs)
            dependencies {
                exclude("/META-INF/services/javax.servlet.ServletContainerInitializer")
                exclude("/ch/qos/logback/classic/servlet/*")
                exclude("/com/epam/drill/agent/instrument/**/*Test.class")
                exclude("/com/epam/drill/agent/instrument/**/*Test$*.class")
                exclude("/com/epam/drill/agent/instrument/**/*TestServer.class")
                exclude("/com/epam/drill/agent/instrument/**/*TestServer$*.class")
                exclude("/com/epam/drill/agent/instrument/**/*TestClient.class")
                exclude("/com/epam/drill/agent/instrument/**/*TestClient$*.class")
            }
        }
        register("integrationTest", Test::class) {
            val intTestAgentLib = targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
                .binaries.getSharedLib(nativeAgentLibName, NativeBuildType.DEBUG)
            val intTestClasspath = jvmIntTestCompilation.runtimeDependencyFiles + jvmIntTestCompilation.output.allOutputs
            val mainClasspath = jvmMainCompilation.runtimeDependencyFiles + jvmMainCompilation.output.allOutputs
            description = "Runs the integration tests using simple native agent"
            group = "verification"
            testClassesDirs = jvmIntTestCompilation.output.classesDirs
            classpath = intTestClasspath - mainClasspath // as main classpath already loaded via agent runtime classes
            jvmArgs = listOf("-agentpath:${intTestAgentLib.outputFile.path}=${runtimeJar.get().outputs.files.singleFile}")
            dependsOn(runtimeJar)
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
            exclude("**/kni")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni")
        }
    }
}
