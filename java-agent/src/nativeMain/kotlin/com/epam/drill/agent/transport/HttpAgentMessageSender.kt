package com.epam.drill.agent.transport

import com.epam.drill.jvmapi.callObjectVoidMethod

object HttpAgentMessageSender {

    fun sendAgentInstance(): Unit =
        callObjectVoidMethod(HttpAgentMessageSender::class, HttpAgentMessageSender::sendAgentInstance)

}
