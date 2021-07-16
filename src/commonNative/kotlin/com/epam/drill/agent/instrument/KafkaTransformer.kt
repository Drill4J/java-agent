package com.epam.drill.agent.instrument

actual object KafkaTransformer {

    const val KAFKA_PRODUCER_INTERFACE = "org/apache/kafka/clients/producer/Producer"
    const val KAFKA_CONSUMER_INTERFACE = "org/apache/kafka/clients/consumer/Consumer"

    actual fun transform(
        className: String,
        classfileBuffer: ByteArray,
        loader: Any?,
    ): ByteArray? {
        return KafkaTransformerStub.transform(className, classfileBuffer, loader)
    }
}
