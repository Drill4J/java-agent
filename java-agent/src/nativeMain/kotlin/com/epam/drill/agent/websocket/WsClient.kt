package com.epam.drill.agent.websocket

import com.epam.drill.jvmapi.callObjectVoidMethodWithString

object WsClient {

    fun connect(adminUrl: String): Unit = callObjectVoidMethodWithString(WsClient::class, WsClient::connect, adminUrl)

}
