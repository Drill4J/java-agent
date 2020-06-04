import com.github.jengelman.gradle.plugins.shadow.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.epam.drill.cross-compilation")
    id("com.epam.drill.version.plugin")
    id("com.github.johnrengelman.shadow")
    distribution
    `maven-publish`
}

val serializationRuntimeVersion = "0.20.0"
val coroutinesVersion = "1.3.5"
val uuidVersion = "0.1.0"

val drillJvmApiLibVersion: String by extra
val drillApiVersion: String by extra
val drillAgentCoreVersion: String by extra
val drillTransportLibVersion: String by extra
val drillLogger: String by extra
val klockVersion: String by extra

allprojects {
    repositories {
        mavenLocal()
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
        maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
        maven(url = "https://dl.bintray.com/kotlin/ktor/")
        mavenCentral()
        jcenter()
    }

    apply(plugin = "com.epam.drill.version.plugin")
    tasks.withType<KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
    tasks.withType<KotlinNativeCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(
                module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
            ).with(
                module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion-native-mt")
            )
        }

    }

}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(
        file("$buildDir/generated/kotlin").apply {
            mkdirs()
            resolve("Version.kt").writeText(
                "package com.epam.drill.agent; internal val agentVersion = \"${project.version}\""
            )
        }
    )
}

val libName = "drill-agent"

kotlin {
    setOf(
        mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } },
        linuxX64(),
        macosX64()
    ).forEach { target ->
        target.binaries { sharedLib(libName, setOf(DEBUG)) }
        target.compilations["test"].cinterops.create("testStubs")
    }

    crossCompilation {

        common {
            defaultSourceSet {
                dependsOn(sourceSets.named("commonMain").get())
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-native:$serializationRuntimeVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties-native:$serializationRuntimeVersion")
                    implementation("com.epam.drill:jvmapi:$drillJvmApiLibVersion")
                    implementation("com.epam.drill.transport:core:$drillTransportLibVersion")
                    implementation("com.benasher44:uuid:$uuidVersion")
                    implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                    implementation("com.epam.drill:common:$drillApiVersion")
                    implementation("com.epam.drill.logger:logger:$drillLogger")
                    implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")
                    implementation("com.epam.drill.knasm:knasm:0.1.0")
                }
            }
        }
    }

    jvm {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect")) //TODO jarhell quick fix for kotlin jvm apps
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("com.epam.drill:common-jvm:$drillApiVersion")
                implementation("com.epam.drill:drill-agent-part-jvm:$drillApiVersion")
                implementation("com.alibaba:transmittable-thread-local:2.11.0")
                implementation("com.epam.drill.logger:logger:$drillLogger")
                implementation("com.soywiz.korlibs.klock:klock-jvm:$klockVersion")
            }
        }
        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.junit.jupiter:junit-jupiter:5.5.2")
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                implementation("com.epam.drill:common:$drillApiVersion")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

    }

}

val jvmJar by tasks.getting(Jar::class) {
    from(provider {
        kotlin.jvm().compilations["main"].compileDependencyFiles.map { if (it.isDirectory) it else zipTree(it) }
    })
}

val agentShadow by tasks.registering(ShadowJar::class) {
    mergeServiceFiles()
    isZip64 = true
    archiveFileName.set("drillRuntime.jar")
    from(jvmJar)
    relocate("kotlin", "kruntime")
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinCommonOptions>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer",
        "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
        "-Xuse-experimental=kotlin.time.ExperimentalTime",
        "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

afterEvaluate {
    val availableTarget =
        kotlin.targets.filterIsInstance<KotlinNativeTarget>().filter { HostManager().isEnabled(it.konanTarget) }

    distributions {
        availableTarget.forEach {
            val name = it.name
            create(name) {
                distributionBaseName.set(name)
                contents {
                    from(agentShadow)
                    from(tasks.getByPath("link${libName.capitalize()}DebugShared${name.capitalize()}")) {
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    }
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
