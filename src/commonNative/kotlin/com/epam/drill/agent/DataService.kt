package com.epam.drill.agent

actual object DataService {
    actual fun createAgentPart(id: String, jarPath: String): Any? {
        return DataServiceStub.createAgentPart(id, jarPath)
    }

    actual fun retrieveClassesData(config: String): ByteArray {
        return DataServiceStub.retrieveClassesData(config)
    }

    actual fun doRawActionBlocking(agentPart: Any, data: String): Any {
        return DataServiceStub.doRawActionBlocking(agentPart, data)
    }
}
