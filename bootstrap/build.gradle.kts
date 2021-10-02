import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("com.epam.drill.gradle.plugin.kni")
    id("com.github.hierynomus.license")
    distribution
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

val nativeTargets = mutableSetOf<KotlinNativeTarget>()
val currentPlatformName = HostManager.host.presetName

kotlin {
    targets {
        nativeTargets.addAll(
            sequenceOf(
                mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } },
                linuxX64(),
                macosX64()
            )
        )
        nativeTargets.forEach { target ->
            if (currentPlatformName == target.name) {
                target.compilations["main"].setCommonSources("src/commonNative")
            }
            target.binaries { sharedLib(libName, setOf(DEBUG)) }
        }
    }
    sourceSets {

        listOf(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.serialization.InternalSerializationApi",
            "io.ktor.utils.io.core.ExperimentalIoApi"
        ).let { annotations ->
            all { annotations.forEach(languageSettings::optIn) }
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kxSerializationVersion")
            }
        }
        val commonNativeDependenciesOnly by creating {
            dependsOn(commonMain)
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
                implementation("io.ktor:ktor-utils:$ktorUtilVersion")
            }
        }

        val posixNative by creating {
            dependsOn(commonNativeDependenciesOnly)
        }
        val linuxX64Main by getting {
            dependsOn(posixNative)
        }
        val mingwX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }
        val macosX64Main by getting {
            dependsOn(posixNative)
        }
    }
}

tasks {
    val generateNativeClasses by getting {}

    //TODO EPMDJ-8696 remove copy
    val otherTargets = nativeTargets.filter { it.name != currentPlatformName }
    val copy = otherTargets.map {
        register<Copy>("bootstrap copy for ${it.name}") {
            from(file("src/commonNative/kotlin"))
            into(file("src/${it.name}Main/kotlin/gen"))
        }
    }
    val bootstrapCopyCommon by registering(DefaultTask::class) {
        group = "build"
        copy.forEach { dependsOn(it) }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        dependsOn(bootstrapCopyCommon)
        dependsOn(generateNativeClasses)
    }
    val cleanExtraData by registering(Delete::class) {
        group = "build"
        otherTargets.forEach {
            val path = "src/${it.name}Main/kotlin/"
            delete(file("${path}kni"), file("${path}gen"))
        }
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
}

//TODO EPMDJ-8696 remove
fun KotlinNativeCompilation.setCommonSources(modulePath: String) {
    defaultSourceSet {
        kotlin.srcDir(file("${modulePath}/kotlin"))
        resources.setSrcDirs(listOf("${modulePath}/resources"))
    }
}
