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
package com.epam.drill.agent.instrument

import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.CADENCE_CONSUMER
import com.epam.drill.agent.CADENCE_PRODUCER
import com.epam.drill.agent.instrument.request.HttpRequest

actual object CadenceTransformer : TransformerObject, AbstractTransformerObject() {

    private val instrumentedMethods = listOf(
        "signalAsync",
        "signalAsyncWithTimeout",
        "start",
        "startAsync",
        "startAsyncWithTimeout",
        "signalWithStart",
    )

    override val logger = KotlinLogging.logger {}

    override fun transform(className: String, ctClass: CtClass) {
        when (className) {
            CADENCE_PRODUCER -> instrumentProducer(ctClass)
            CADENCE_CONSUMER -> instrumentConsumer(ctClass)
        }
    }

    private fun instrumentProducer(ctClass: CtClass) {
        ctClass.constructors
            .mapNotNull { constructor ->
                constructor.parameterTypes
                    .indexOfFirst { it.name.replace(".", "/") == "com/uber/cadence/client/WorkflowOptions" }
                    .takeIf { it >= 0 }
                    ?.let { constructor to it + 1 }  // 0 - index of "this" object
            }
            .forEach { (constructor, paramIndex) ->
                constructor.insertBefore(
                    """
                    if ($$paramIndex.getMemo() == null) {
                        $$paramIndex = new com.uber.cadence.client.WorkflowOptions.Builder($$paramIndex).setMemo(new java.util.HashMap()).build();
                    }
                    """.trimIndent()
                )
            }

        instrumentedMethods
            .mapNotNull { method ->
                ctClass
                    .runCatching { this.getDeclaredMethod(method) }
                    .onFailure { logger.error(it) { "instrumentProducer: Method `$method` not found, check cadence api version" } }
                    .getOrNull()
            }
            .forEach { method ->
                method.insertBefore(
                    """
                    java.util.Map drillHeaders = ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::loadDrillHeaders.name}();
                    if (drillHeaders != null) {
                        java.util.Iterator iterator = drillHeaders.entrySet().iterator();
                         if (getOptions().isPresent()) {
                            com.uber.cadence.client.WorkflowOptions options = (com.uber.cadence.client.WorkflowOptions) getOptions().get();
                            if (options.getMemo() != null) {
                                while (iterator.hasNext()) {
                                    java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                                    String key = ((String) entry.getKey());
                                    if (options.getMemo().get(key) == null) {
                                        options.getMemo().put(key, entry.getValue());
                                    }
                                }
                            }
                         }
                    }
                    """.trimIndent()
                )
            }
    }

    private fun instrumentConsumer(ctClass: CtClass) {
        ctClass.getDeclaredMethod("run").insertBefore(
            """
            java.util.Map drillHeaders = new java.util.HashMap();
            com.uber.cadence.Memo memo = attributes.getMemo();
            if (memo != null) {
                java.util.Map fields = memo.getFields();
                if (fields != null) {
                    java.util.Iterator iterator = fields.entrySet().iterator(); 
                    while (iterator.hasNext()) {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                        String key = ((String) entry.getKey());
                        if (key.startsWith("${HttpRequest.DRILL_HEADER_PREFIX}")) {
                            java.nio.ByteBuffer byteBuffer = (java.nio.ByteBuffer) entry.getValue(); 
                            if (byteBuffer != null) {
                                final byte[] valueBytes = new byte[byteBuffer.remaining()];
                                byteBuffer.mark(); 
                                byteBuffer.get(valueBytes); 
                                byteBuffer.reset();
                                String value = (String) com.uber.cadence.converter.JsonDataConverter.getInstance().fromData(valueBytes, String.class, String.class);
                                drillHeaders.put(key, value);
                            }
                        }
                    }
                    ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::storeDrillHeaders.name}(drillHeaders);
                }
            }
            """.trimIndent()
        )
    }

}
