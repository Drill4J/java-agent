plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.alibaba:transmittable-thread-local:2.11.0")
}

tasks {
    named<Jar>("jar") {
        archiveFileName.set("drill-proxy.jar")
        manifest {
            attributes["Premain-Class"] = "com.epam.drill.agent.ProxyAgentKt"
            attributes["Can-Redefine-Classes"] = true
            attributes["Can-Retransform-Classes"] = true
        }
        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)
        from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar")}.map { zipTree(it) } })
    }
}