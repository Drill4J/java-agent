/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.instrument.AbstractPropagationTransformer
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_KAFKA_ENABLED
import com.epam.drill.agent.instrument.KAFKA_CONSUMER_SPRING
import com.epam.drill.agent.instrument.KAFKA_PRODUCER_INTERFACE

/**
 * Transformer for Kafka producer and Spring Kafka listener
 *
 * Tested with:
 *     org.apache.kafka:kafka-clients:3.2.3
 *     org.springframework.kafka:spring-kafka:2.9.13
 */
abstract class KafkaTransformerObject(agentConfiguration: AgentConfiguration) : HeadersProcessor,
    AbstractPropagationTransformer(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun enabled(): Boolean {
        return super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_KAFKA_ENABLED]
    }

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        KAFKA_CONSUMER_SPRING == className || interfaces.contains(KAFKA_PRODUCER_INTERFACE)

    override fun transform(className: String, ctClass: CtClass) {
        val interfaces = ctClass.interfaces.map(CtClass::getName)
        when {
            interfaces.contains(KAFKA_PRODUCER_INTERFACE.replace("/", ".")) -> instrumentProducer(ctClass)
            className == KAFKA_CONSUMER_SPRING -> instrumentConsumer(ctClass)
            //TODO add Consumer for Kafka EPMDJ-8488
        }
    }

    private fun instrumentProducer(ctClass: CtClass) {
        ctClass.getDeclaredMethods("send").forEach {
            it.insertCatching(
                CtBehavior::insertBefore,
                """
                java.util.Map drillHeaders = ${this::class.java.name}.INSTANCE.${this::retrieveHeaders.name}();
                if (drillHeaders != null) {
                    java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                    while (iterator.hasNext()) {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                        String key = ((String) entry.getKey());
                        if (!$1.headers().headers(key).iterator().hasNext()) {
                            $1.headers().add(key, ((String) entry.getValue()).getBytes());
                        }
                    }
                }
                """.trimIndent()
            )
        }
    }

    private fun instrumentConsumer(ctClass: CtClass) {
        ctClass.getDeclaredMethods("doInvokeRecordListener").forEach {
            it.insertCatching(
                CtBehavior::insertBefore,
                """
                java.util.Iterator headers = $1.headers().iterator();
                java.util.Map drillHeaders = new java.util.HashMap();
                while (headers.hasNext()) {
                    org.apache.kafka.common.header.Header header = (org.apache.kafka.common.header.Header) headers.next();
                    if (header.key().startsWith("${HeadersProcessor.DRILL_HEADER_PREFIX}")) {
                        drillHeaders.put(header.key(), new String(header.value()));
                    }    
                }
                ${this::class.java.name}.INSTANCE.${this::storeHeaders.name}(drillHeaders);
                """.trimIndent()
            )
        }
    }

}
