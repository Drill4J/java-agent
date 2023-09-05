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
}

group = "com.epam.drill"
version = rootProject.version

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val jacocoVersion: String by parent!!.extra
val atomicfuVersion: String by parent!!.extra
val lubenZstdVersion: String by parent!!.extra
val bcelVersion: String by parent!!.extra
val microutilsLoggingVersion: String by parent!!.extra
val logbackVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    compileOnly("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")

    implementation("io.aesy:aesy:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:$kotlinxCollectionsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    implementation("org.jacoco:org.jacoco.core:$jacocoVersion")
    implementation("org.apache.bcel:bcel:$bcelVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$microutilsLoggingVersion")
    implementation("com.github.luben:zstd-jni:$lubenZstdVersion")

    implementation(project(":jacoco"))
    implementation(project(":common"))
    implementation(project(":test2code-common"))
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
    test {
        useJUnitPlatform()
    }
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
