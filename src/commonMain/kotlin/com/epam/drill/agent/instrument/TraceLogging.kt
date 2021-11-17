package com.epam.drill.agent.instrument

expect object TraceLogging {
    fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray?
}
