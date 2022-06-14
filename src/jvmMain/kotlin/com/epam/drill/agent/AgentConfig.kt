package com.epam.drill.agent

import com.epam.drill.kni.*

@Kni
actual object AgentConfig {
    actual external fun drillInstallationDir(): String?
}