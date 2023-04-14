import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.operation.BranchListOp

plugins {
    kotlin("jvm").apply(false)
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").apply(false)
    id("org.ajoberstar.grgit")
    id("com.github.hierynomus.license").apply(false)
    id("com.github.johnrengelman.shadow").apply(false)
    id("com.epam.drill.gradle.plugin.kni").apply(false)
    id("com.epam.drill.agent.runner.app").apply(false)
}

group = "com.epam.drill"

val kotlinVersion: String by extra
val kotlinxCollectionsVersion: String by extra
val kotlinxCoroutinesVersion: String by extra
val kotlinxSerializationVersion: String by extra

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    val constraints = setOf(
        dependencies.constraints.create("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"),
        dependencies.constraints.create("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"),
        dependencies.constraints.create("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"),
        dependencies.constraints.create("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion"),
        dependencies.constraints.create("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:$kotlinxCollectionsVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$kotlinxCoroutinesVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion"),
        dependencies.constraints.create("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion"),
    )
    configurations.all {
        dependencyConstraints += constraints
    }
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val sharedLibsDir = file("$projectDir/lib-jvm-shared")
    val sharedLibsRef: String by extra
    val updateSharedLibs by registering {
        group = "other"
        doLast {
            val gitrepo = Grgit.open { dir = sharedLibsDir }
            val branches = gitrepo.branch.list { mode = BranchListOp.Mode.LOCAL }
            val branchToName: (Branch) -> String = { it.name }
            gitrepo.fetch()
            gitrepo.checkout {
                branch = sharedLibsRef
                createBranch = !branches.map(branchToName).contains(sharedLibsRef)
            }
            gitrepo.pull()
        }
    }
}
