package com.epam.drill.request

import com.alibaba.ttl.*
import com.epam.drill.plugin.*
import com.epam.drill.session.*
import kotlinx.serialization.protobuf.*
import mu.*
import kotlin.reflect.jvm.*

actual object RequestHolder {
    private val logger = KotlinLogging.logger(RequestHolder::class.jvmName)

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

}
