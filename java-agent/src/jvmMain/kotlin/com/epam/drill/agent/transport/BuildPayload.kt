package com.epam.drill.agent.transport

import com.epam.drill.agent.common.transport.AgentMessage
import kotlinx.serialization.Serializable

@Serializable
class BuildPayload(
    val groupId: String,
    val appId: String,
    val commitSha: String? = null,
    val buildVersion: String? = null,
    val branch: String? = null,
    val commitDate: String? = null,
    val commitMessage: String? = null,
    val commitAuthor: String? = null
) : AgentMessage()