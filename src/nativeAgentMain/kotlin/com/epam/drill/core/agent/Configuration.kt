package com.epam.drill.core.agent

import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.serialization.*

fun performAgentInitialization(initialParams: Map<String, String>) {
    val adminAddress = initialParams.getValue("adminAddress")
    val agentId = initialParams.getValue("agentId")
    val buildVersion = initialParams["buildVersion"] ?: ""
    val serviceGroupId = initialParams["serviceGroupId"] ?: ""
    val drillInstallationDir = initialParams.getValue("drillInstallationDir")
    exec {
        this.drillInstallationDir = drillInstallationDir
        this.agentConfig = AgentConfig(agentId, buildVersion, serviceGroupId)
        this.adminAddress = URL("ws://$adminAddress")
    }
}

fun calculateBuildVersion() {
    val agentConfig = exec { agentConfig }
    if (agentConfig.buildVersion.isEmpty()) {
        val initializerClass = FindClass("com/epam/drill/ws/Initializer")
        val selfMethodId: jfieldID? =
            GetStaticFieldID(initializerClass, "INSTANCE", "Lcom/epam/drill/ws/Initializer;")
        val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
        val calculateBuild: jmethodID? = GetMethodID(initializerClass, "calculateBuild", "()I")
        val buildVersion = CallIntMethod(initializer, calculateBuild)

        agentConfig.buildVersion = buildVersion.toString()
    }
    DLogger("BuildVersionLogger").info { "Calculated build version: ${agentConfig.buildVersion}" }
}

fun getClassesByConfig(): List<String> {
    val packagesPrefixes = exec { agentConfig.packagesPrefixes }
    val classLoadingUtilClass = FindClass("com/epam/drill/ws/ClassLoadingUtil")
    val selfMethodId: jfieldID? =
        GetStaticFieldID(classLoadingUtilClass, "INSTANCE", "Lcom/epam/drill/ws/ClassLoadingUtil;")
    val classLoadingUtil: jobject? = GetStaticObjectField(classLoadingUtilClass, selfMethodId)
    val retrieveClassesData: jmethodID? =
        GetMethodID(classLoadingUtilClass, "retrieveClassesData", "(Ljava/lang/String;)Ljava/lang/String;")
    val jsonClasses = CallObjectMethod(classLoadingUtil, retrieveClassesData, NewStringUTF(packagesPrefixes))
    return String.serializer().list parse (jsonClasses.toKString() ?: "[]")
}

fun setPackagesPrefixes(prefixes: String) {
    exec { agentConfig.packagesPrefixes = prefixes }
}