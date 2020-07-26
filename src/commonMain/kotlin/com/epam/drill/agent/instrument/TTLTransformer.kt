package com.epam.drill.agent.instrument

expect object TTLTransformer {
    fun transform(
        loader: Any?,
        classFile: String?,
        classBeingRedefined: Any?,
        classFileBuffer: ByteArray
    ): ByteArray?
}
