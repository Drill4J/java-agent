package com.epam.drill.agent

expect object DataService {
    fun createAgentPart(id: String, jarPath: String): Any?
    fun retrieveClassesData(config: String): ByteArray
    fun doRawActionBlocking(agentPart: Any, data: String): Any
}
