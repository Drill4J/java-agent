plugins {
    java
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("com.alibaba:transmittable-thread-local:2.11.0")
}

tasks {
    shadowJar {
        archiveFileName.set("drill-proxy.jar")
        manifest {
            attributes["Premain-Class"] = "com.epam.drill.agent.proxy.ProxyAgent"
            attributes["Can-Redefine-Classes"] = true
            attributes["Can-Retransform-Classes"] = true
        }
        dependencies {
            exclude("/META-INF/**")
        }
    }
}
