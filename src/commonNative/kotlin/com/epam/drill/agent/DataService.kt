package com.epam.drill.agent

import com.epam.drill.agent.jvmapi.*
import com.epam.drill.jvmapi.gen.*

actual object DataService {

    actual fun retrieveClassesData(config: String): ByteArray {
        val (serviceClass, service) = instance<DataService>()
        val retrieveClassesData: jmethodID? =
            GetMethodID(serviceClass, DataService::retrieveClassesData.name, "(Ljava/lang/String;)[B")
        return CallObjectMethod(service, retrieveClassesData, NewStringUTF(config)).readBytes()!!
    }
}
