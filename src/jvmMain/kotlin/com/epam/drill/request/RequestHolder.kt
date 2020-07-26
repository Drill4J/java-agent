package com.epam.drill.request

import com.alibaba.ttl.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.*
import com.epam.drill.session.*
import kotlinx.serialization.protobuf.*
import kotlin.reflect.jvm.*

@Kni
actual object RequestHolder {
    private val logger = Logging.logger(RequestHolder::class.jvmName)

    init {
        threadStorage = TransmittableThreadLocal()
    }

    actual fun store(drillRequest: ByteArray) {
        val drillRequestObject = ProtoBuf.load(DrillRequest.serializer(), drillRequest)
        threadStorage.set(drillRequestObject)
        logger.trace { "session ${drillRequestObject.drillSessionId} saved" }
    }

    actual fun dump(): ByteArray? {
        return threadStorage.get()?.let { ProtoBuf.dump(DrillRequest.serializer(), it) }
    }

    actual fun closeSession() {
        logger.trace { "session ${threadStorage.get()} closed" }
        threadStorage.remove()
    }

}
