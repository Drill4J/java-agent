package com.epam.drill.agent.instrument

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object KafkaTransformer {

    private const val kafkaHeaders = "drill-kafka-header-key"

    private val logger = Logging.logger(KafkaTransformer::class.jvmName)

    actual fun transform(
        className: String,
        classfileBuffer: ByteArray,
        loader: Any?,
    ): ByteArray? {
        return try {
            ClassPool.getDefault().appendClassPath(LoaderClassPath(loader as? ClassLoader))
            when (className) {
                "org/apache/kafka/clients/producer/Producer" -> producerInstrument(classfileBuffer)
                "org/apache/kafka/clients/consumer/Consumer" -> consumerInstrument(classfileBuffer)
                else -> null
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }

    private fun producerInstrument(
        classfileBuffer: ByteArray,
    ) = ClassPool.getDefault().makeClass(ByteArrayInputStream(classfileBuffer))?.run {
        getDeclaredMethods("send").forEach {
            it.insertBefore("""
                byte[] headers = ${RequestHolder::class.java.name}.INSTANCE.${RequestHolder::dump.name}();
                if (headers != null) {
                    $1.headers().add("$kafkaHeaders", headers);
                }
            """.trimIndent())
        }
        toBytecode()
    }


    private fun consumerInstrument(
        classfileBuffer: ByteArray,
    ) = ClassPool.getDefault().makeClass(ByteArrayInputStream(classfileBuffer))?.run {
        getDeclaredMethods("poll").forEach {
            it.insertAfter("""
                java.util.Iterator records = ${'$'}_.iterator(); 
                while (records.hasNext()) {
                    org.apache.kafka.clients.consumer.ConsumerRecord record = (org.apache.kafka.clients.consumer.ConsumerRecord) records.next();
                    java.util.Iterator headers = record.headers().headers("$kafkaHeaders").iterator();
                    while (headers.hasNext()) {
                        org.apache.kafka.common.header.Header header = (org.apache.kafka.common.header.Header) headers.next();
                        ${RequestHolder::class.java.name}.INSTANCE.${RequestHolder::store.name}(header.value());   
                    }
                }
            """.trimIndent())
        }
        toBytecode()
    }

}
