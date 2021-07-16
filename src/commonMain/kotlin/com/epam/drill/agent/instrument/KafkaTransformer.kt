package com.epam.drill.agent.instrument

expect object KafkaTransformer {
    fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray?
}
