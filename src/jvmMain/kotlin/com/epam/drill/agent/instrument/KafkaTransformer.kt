/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent.instrument

import com.epam.drill.*
import com.epam.drill.agent.instrument.util.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import javassist.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object KafkaTransformer {

    private val logger = Logging.logger(KafkaTransformer::class.jvmName)

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        runCatching {
            when (className) {
                KAFKA_PRODUCER_INTERFACE -> ctClass.producerInstrument()
                KAFKA_CONSUMER_SPRING -> ctClass.consumerInstrument()
                //todo add Consumer for Kafka EPMDJ-8488
                else -> null
            }
        }.onFailure {
            logger.warn(it) { "Instrumentation error" }
        }.getOrNull()
    }

    private fun CtClass.producerInstrument() = run {
        getDeclaredMethods("send").forEach {
            it.wrapCatching(
                CtMethod::insertBefore,
                """
                java.util.Map drillHeaders = ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::loadDrillHeaders.name}();
                if (drillHeaders != null) {
                    java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                    while (iterator.hasNext()) {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                        $1.headers().add(((String) entry.getKey()), ((String) entry.getValue()).getBytes());
                    }
                }
            """.trimIndent()
            )
        }
        toBytecode()
    }


    private fun CtClass.consumerInstrument() = run {
        getDeclaredMethods("doInvokeRecordListener").forEach {
            it.wrapCatching(
                CtMethod::insertBefore,
                """
                java.util.Iterator headers = $1.headers().iterator();
                java.util.Map drillHeaders = new java.util.HashMap();
                while (headers.hasNext()) {
                    org.apache.kafka.common.header.Header header = (org.apache.kafka.common.header.Header) headers.next();
                    if (header.key().startsWith("drill-")) {
                        drillHeaders.put(header.key(), new String(header.value()));
                    }    
                }
                ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(drillHeaders);
            """.trimIndent()
            )
        }
        toBytecode()
    }

}
