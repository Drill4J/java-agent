plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.epam.drill.cross-compilation")
    id("com.github.johnrengelman.shadow")
    id("com.epam.drill.gradle.plugin.kni")
    id("com.github.hierynomus.license")
    distribution
    `maven-publish`
}

val scriptUrl: String by extra

val kxSerializationVersion: String by extra
val kxCoroutinesVersion: String by extra
val uuidVersion: String by extra

val drillJvmApiLibVersion: String by extra
val drillApiVersion: String by extra
val drillAgentCoreVersion: String by extra
val drillTransportLibVersion: String by extra
val drillLogger: String by extra
val knasmVersion: String by extra
val kniVersion: String by extra
val ktorUtilVersion: String by extra


val libName = "drill-agent-bootstrap"
val kniOutputDir = "src/kni/kotlin"

kotlin {
    setOf(
        mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } },
        linuxX64(),
        macosX64()
    ).forEach { target ->
        target.binaries { sharedLib(libName, setOf(DEBUG)) }
        target.compilations["test"].cinterops.create("testStubs")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kxSerializationVersion")
            }
        }
    }
    crossCompilation {
        common {
            defaultSourceSet {
                dependsOn(sourceSets.named("commonMain").get())
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kxSerializationVersion")
                    implementation("com.epam.drill:jvmapi:$drillJvmApiLibVersion")
                    implementation("com.benasher44:uuid:$uuidVersion")
                    implementation("com.epam.drill:drill-agent-part:$drillApiVersion")
                    implementation("com.epam.drill:common:$drillApiVersion")
                    implementation("com.epam.drill.logger:logger:$drillLogger")
                    implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")
                    implementation("com.epam.drill.knasm:knasm:$knasmVersion")
                    implementation("com.epam.drill.kni:runtime:$kniVersion")
                    implementation("io.ktor:ktor-utils-native:$ktorUtilVersion")
                }
            }
        }
    }
}

tasks {
    val generateNativeClasses by getting {}
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        dependsOn(generateNativeClasses)
    }
    val cleanExtraData by registering(Delete::class) {
        group = "build"
        delete(kniOutputDir)
    }

    clean {
        dependsOn(cleanExtraData)
    }
}

afterEvaluate {
    val availableTargets =
        kotlin.targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().filter {
            org.jetbrains.kotlin.konan.target.HostManager()
                .isEnabled(it.konanTarget)
        }

    distributions {
        availableTargets.forEach {
            val name = it.name
            create(name) {
                distributionBaseName.set(name)
                contents {
                    from(tasks.named("link${libName.capitalize()}DebugShared${name.capitalize()}")) {
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    }
                }
            }
        }
    }
    publishing {
        publications {
            availableTargets.forEach {
                create<MavenPublication>("${it.name}Zip") {
                    artifactId = "$libName-${it.name}"
                    artifact(tasks["${it.name}DistZip"])
                }
            }
        }
    }
}
