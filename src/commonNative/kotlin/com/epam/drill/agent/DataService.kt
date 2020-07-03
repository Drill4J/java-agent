package com.epam.drill.agent

import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*

actual object DataService {
    actual fun createAgentPart(id: String, jarPath: String): Any? {
        DataService::createAgentPart
        val (serviceClass, service) = instance<DataService>()
        val method: jmethodID? = GetMethodID(
            serviceClass,
            DataService::createAgentPart.name,
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;"
        )
        return CallObjectMethod(service, method, NewStringUTF(id), NewStringUTF(jarPath))
    }

    actual fun retrieveClassesData(config: String): ByteArray {
        val (serviceClass, service) = instance<DataService>()
        val retrieveClassesData: jmethodID? = GetMethodID(
            serviceClass,
            DataService::retrieveClassesData.name,
            "(Ljava/lang/String;)[B"
        )
        return CallObjectMethod(service, retrieveClassesData, NewStringUTF(config)).readBytes()!!
    }

}
