package com.epam.drill.agent

expect object AgentConfig {
    fun drillInstallationDir(): String?
}