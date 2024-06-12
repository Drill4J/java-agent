import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.noarg")
    kotlin("plugin.serialization")
    id("kotlinx-atomicfu")
    id("com.github.hierynomus.license")
    id("com.google.protobuf") version "0.9.4"
}

group = "com.epam.drill"
version = rootProject.version

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val jacocoVersion: String by parent!!.extra
val atomicfuVersion: String by parent!!.extra
val bcelVersion: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra
val logbackVersion: String by parent!!.extra
val dataIngestApiProtoUrl: String by parent!!.extra
val protobufJavaVersion = "4.27.1"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {

    compileOnly("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:$kotlinxCollectionsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    implementation("org.jacoco:org.jacoco.core:$jacocoVersion")
    implementation("org.apache.bcel:bcel:$bcelVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufJavaVersion")

    implementation(project(":common"))
    implementation(project(":test2code-common"))
    implementation(project(":test2code-jacoco"))
    implementation(project(":konform"))

    testImplementation(kotlin("test-junit"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.Experimental")
    languageSettings.optIn("kotlin.ExperimentalStdlibApi")
    languageSettings.optIn("kotlin.time.ExperimentalTime")
    languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
    languageSettings.optIn("kotlinx.coroutines.FlowPreview")
    languageSettings.optIn("kotlinx.coroutines.InternalCoroutinesApi")
    languageSettings.optIn("kotlinx.coroutines.ObsoleteCoroutinesApi")
    languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
}

@Suppress("UNUSED_VARIABLE")
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    // protobuf class generation tasks
    val protoPath = "src/main/proto"
    register<de.undercouch.gradle.tasks.download.Download>("downloadProto") {
        val destinationFile = file("$protoPath/data-ingest.proto")
        src(dataIngestApiProtoUrl)
        dest(destinationFile)
        onlyIfModified(true)
        doLast {
            println("Downloaded $dataIngestApiProtoUrl to $destinationFile")
        }
    }
    generateProto {
        dependsOn("downloadProto")
    }
    compileKotlin {
        dependsOn("generateProto")
    }
    register("cleanProto") {
        doLast {
            delete(file(protoPath))
        }
    }
    named("clean") {
        dependsOn("cleanProto")
    }
}

noArg {
    annotation("kotlinx.serialization.Serializable")
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
    exclude("**/build/generated/proto/**/*.java")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufJavaVersion"
    }
}
