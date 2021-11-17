package com.epam.drill.agent.instrument

actual object TraceLogging {
    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = TraceLoggingStub.transform(className, classFileBuffer, loader, protectionDomain)
}
