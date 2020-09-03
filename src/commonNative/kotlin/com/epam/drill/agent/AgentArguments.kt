package com.epam.drill.agent

import com.benasher44.uuid.*
import com.epam.drill.logger.api.*
import kotlinx.serialization.*

@Serializable
data class AgentArguments(
    val agentId: String,
    val adminAddress: String,
    val drillInstallationDir: String = javaProcess().firstAgentPath,
    val buildVersion: String = "unspecified",
    val instanceId: String = uuid4().toString(),
    val groupId: String = "",
    val logLevel: String = LogLevel.ERROR.name,
    val logFile: String? = null,
    val isWebApp: Boolean = false,
    val isAsyncApp: Boolean = false,
    val webAppNames: String = "",
    val classScanDelay: Long = 0L
) {
    val webApps: List<String>
        get() = webAppNames.split(":")
}
