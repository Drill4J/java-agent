import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.noarg")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
    application
    id("edu.sc.seis.launch4j") version "3.0.6"
}

group = "com.epam.drill.agent"
version = rootProject.version

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val kotlinxCliVersion: String by parent!!.extra
val jacocoVersion: String by parent!!.extra
val bcelVersion: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra
val logbackVersion: String by parent!!.extra

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:$kotlinxCollectionsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
    implementation("org.jacoco:org.jacoco.core:$jacocoVersion")
    implementation("org.apache.bcel:bcel:$bcelVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")

    implementation(project(":test2code"))
    implementation(project(":common"))
    implementation(project(":test2code-common"))
    implementation(project(":test2code-jacoco"))
    implementation(project(":konform"))
    implementation(project(":agent-transport"))

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
}

application {
    mainClass.set("com.epam.drill.agent.archive.scanner.AppArchiveScannerCliKt")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("fat")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

launch4j {
    mainClassName.set(application.mainClass.get())
    bundledJrePath.set(null as String?)
}
