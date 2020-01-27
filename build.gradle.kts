import com.github.jengelman.gradle.plugins.shadow.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.konan.target.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
    distribution
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "5.1.0"
}
val gccIsNeeded = (project.property("gccIsNeeded") as String).toBoolean()

allprojects {

    repositories {
        mavenCentral()
        jcenter()
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
        maven(url = "https://dl.bintray.com/kotlin/ktor/")
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    }

}

val libName = "drill-agent"
val nativeTargets = mutableSetOf<KotlinNativeTarget>()

kotlin {
    targets {
        if (isDevMode) {
            currentTarget("nativeAgent") {
                binaries { sharedLib(libName, setOf(DEBUG)) }
            }.apply {
                nativeTargets.add(this)
            }
        } else {
            mingwX64 { binaries { sharedLib(libName, setOf(DEBUG)) } }.apply { nativeTargets.add(this) }
            macosX64 { binaries { sharedLib(libName, setOf(DEBUG)) } }.apply { nativeTargets.add(this) }
            linuxX64 {
                binaries {
                    if (!gccIsNeeded) sharedLib(libName, setOf(DEBUG))
                    else staticLib(libName, setOf(DEBUG))
                }
            }.apply {
                nativeTargets.add(this)
            }
        }
        jvm("javaAgent")

    }
    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        val cinterops = it.compilations["test"].cinterops
        cinterops?.create("jvmapiStub")
        cinterops?.create("testSocket")
    }

    sourceSets {
        val commonNativeMain = maybeCreate("nativeAgentMain")
        @Suppress("UNUSED_VARIABLE") val commonNativeTest = maybeCreate("nativeAgentTest")
        if (!isDevMode) {
            nativeTargets.forEach {
                it.compilations.forEach { knCompilation ->
                    if (knCompilation.name == "main")
                        knCompilation.defaultSourceSet { dependsOn(commonNativeMain) }
                    else
                        knCompilation.defaultSourceSet { dependsOn(commonNativeTest) }

                }
            }
        }
        jvm("javaAgent").compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect")) //TODO jarhell quick fix for kotlin jvm apps
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation(project(":common"))
                implementation(project(":plugin-api:drill-agent-part"))
                implementation("com.alibaba:transmittable-thread-local:2.11.0")
            }
        }
        jvm("javaAgent").compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation(project(":common"))
                implementation(project(":plugin-api:drill-agent-part"))
            }
        }
        named("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        named("nativeAgentMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
                implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVerison")
                implementation("com.epam.drill.transport:core:0.1.0")
                implementation("com.benasher44:uuid:0.0.6")
                implementation(project(":plugin-api:drill-agent-part"))
                implementation(project(":common"))
                implementation(project(":agent:core"))
                implementation(project(":agent:util"))
            }
        }
    }

}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.io.core.ExperimentalIoApi"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}
tasks {

    val agentShadow by registering(ShadowJar::class) {
        mergeServiceFiles()
        isZip64 = true
        relocate("kotlin", "kruntime")
        archiveFileName.set("drillRuntime.jar")
        from(javaAgentJar)
    }

    named<Jar>("javaAgentJar") {
        archiveFileName.set("drillRuntime-temp.jar")
        from(provider {
            kotlin.targets["javaAgent"].compilations["main"].compileDependencyFiles.map {
                if (it.isDirectory) it else zipTree(it)
            }
        })
    }
    if (gccIsNeeded)
        register("link${libName.capitalize()}DebugSharedLinuxX64", Exec::class) {
            mustRunAfter("link${libName.capitalize()}DebugStaticLinuxX64")
            group = LifecycleBasePlugin.BUILD_GROUP
            val linuxTarget = kotlin.targets["linuxX64"] as KotlinNativeTarget
            val linuxStaticLib = linuxTarget
                .binaries
                .findStaticLib(libName, NativeBuildType.DEBUG)!!
                .outputFile.toPath()

            val linuxBuildDir = linuxStaticLib.parent.parent
            val targetSo = linuxBuildDir.resolve("${libName}DebugShared").resolve("lib${libName.replace("-", "_")}.so")
            outputs.file(targetSo)
            doFirst {

                targetSo.parent.toFile().mkdirs()
                commandLine = listOf(
                    "docker-compose",
                    "run",
                    "--rm",
                    "gcc",
                    "-shared",
                    "-o",
                    "/home/project/${project.name}/${projectDir.toPath().relativize(targetSo)
                        .toString()
                        .replace(
                            "\\",
                            "/"
                        )}",
                    "-Wl,--whole-archive",
                    "/home/project/${project.name}/${projectDir.toPath().relativize(linuxStaticLib)
                        .toString()
                        .replace(
                            "\\",
                            "/"
                        )}",
                    "-Wl,--no-whole-archive",
                    "-static-libgcc",
                    "-static-libstdc++",
                    "-lstdc++"
                )

            }
        }
}


val javaAgentJar: Jar by tasks
val agentShadow: ShadowJar by tasks

afterEvaluate {
    val availableTarget = nativeTargets.filter { HostManager().isEnabled(it.konanTarget) }

    distributions {
        availableTarget.forEach {
            val name = it.name
            create(name) {
                baseName = name
                contents {
                    from(tasks.getByPath(":agent:java:proxy-agent:jar"))
                    from(agentShadow)
                    from(tasks.getByPath("link${libName.capitalize()}DebugShared${name.capitalize()}"))
                }
            }
        }
    }
    publishing {
        repositories {
            maven {

                url = uri("http://oss.jfrog.org/oss-release-local")
                credentials {
                    username =
                        if (project.hasProperty("bintrayUser"))
                            project.property("bintrayUser").toString()
                        else System.getenv("BINTRAY_USER")
                    password =
                        if (project.hasProperty("bintrayApiKey"))
                            project.property("bintrayApiKey").toString()
                        else System.getenv("BINTRAY_API_KEY")
                }
            }
        }

        publications {
            availableTarget.forEach {
                create<MavenPublication>("${it.name}Zip") {
                    artifactId = "$libName-${it.name}"
                    artifact(tasks["${it.name}DistZip"])
                }
            }
        }
    }
}
