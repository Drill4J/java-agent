package com.epam.drill.agent.transport

import com.epam.drill.agent.common.transport.AgentMessage
import kotlinx.serialization.Serializable

@Serializable
class InstancePayload(
    val groupId: String,
    val appId: String,
    val instanceId: String,
    val commitSha: String? = null,
    val buildVersion: String? = null,
    val envId: String? = null,
): AgentMessage()