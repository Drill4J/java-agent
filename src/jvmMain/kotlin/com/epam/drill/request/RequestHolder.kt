package com.epam.drill.request

import com.alibaba.ttl.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.protobuf.*
import kotlin.reflect.jvm.*

@Kni
actual object RequestHolder {
    private val logger = Logging.logger(RequestHolder::class.jvmName)

    private lateinit var threadStorage: InheritableThreadLocal<DrillRequest>

    val agentContext: AgentContext = RequestAgentContext { threadStorage.get() }

    actual fun init(isAsync: Boolean) {
        threadStorage = if (isAsync) TransmittableThreadLocal() else InheritableThreadLocal()
    }

    actual fun store(drillRequest: ByteArray) {
        store(ProtoBuf.load(DrillRequest.serializer(), drillRequest))

    }

    fun store(drillRequest: DrillRequest) {
        threadStorage.set(drillRequest)
        logger.trace { "session ${drillRequest.drillSessionId} saved" }
    }

    actual fun dump(): ByteArray? {
        return threadStorage.get()?.let { ProtoBuf.dump(DrillRequest.serializer(), it) }
    }

    actual fun closeSession() {
        logger.trace { "session ${threadStorage.get()} closed" }
        threadStorage.remove()
    }
}
