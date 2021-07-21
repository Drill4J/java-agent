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

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object CadenceTransformer {

    private const val cadenceHeaders = "drill-cadence-header-key"

    private val logger = Logging.logger(CadenceTransformer::class.jvmName)

    actual fun transform(
        className: String,
        classfileBuffer: ByteArray,
        loader: Any?,
    ): ByteArray? {
        return try {
            ClassPool.getDefault().appendClassPath(LoaderClassPath(loader as? ClassLoader))
            when (className) {
                "com/uber/cadence/internal/sync/WorkflowStubImpl" -> producerInstrument(classfileBuffer)
//                "com/uber/cadence/client/WorkflowStub" -> producerInstrument(classfileBuffer)
                "com/uber/cadence/internal/sync/WorkflowRunnable" -> consumerInstrument(classfileBuffer)
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
        // getConstructor("(Lcom/uber/cadence/client/WorkflowClientOptions;Lcom/uber/cadence/internal/external/GenericWorkflowClientExternal;Ljava/lang/String;Lcom/uber/cadence/client/WorkflowOptions;)V")
        val constructors: List<Pair<CtConstructor, Int>> = constructors.mapNotNull { constructor ->
            constructor.parameterTypes.indexOfFirst { clazz ->
                clazz.name.replace(".", "/") == "com/uber/cadence/client/WorkflowOptions"
            }.takeIf { it >= 0 }?.let { constructor to it + 1 /* 0 - index is "this" object */ }
        }
        constructors.forEach { (constructor, paramIndex) ->
            constructor.insertBefore("""
                if ($$paramIndex.getMemo() == null) {
                    $$paramIndex = new com.uber.cadence.client.WorkflowOptions.Builder($$paramIndex).setMemo(new java.util.HashMap()).build();
                }
            """.trimIndent())
        }
        //TODO validate on method not found
        listOf(
            getDeclaredMethod("signalAsync"),
            getDeclaredMethod("signalAsyncWithTimeout"),
            getDeclaredMethod("start"),
            getDeclaredMethod("startAsync"),
            getDeclaredMethod("startAsyncWithTimeout"),
            getDeclaredMethod("signalWithStart"),
        ).forEach {
            it.insertBefore("""
                byte[] headers = ${RequestHolder::class.java.name}.INSTANCE.${RequestHolder::dump.name}();
                if (headers != null) {
                    if (getOptions().isPresent()) {
                       com.uber.cadence.client.WorkflowOptions options = (com.uber.cadence.client.WorkflowOptions) getOptions().get();
                       if (options.getMemo() != null) {
                            options.getMemo().put("$cadenceHeaders",java.util.Base64.getEncoder().encodeToString(headers));
                       }
                    }
                }
            """.trimIndent())
        }
        toBytecode()
    }

    private fun consumerInstrument(
        classfileBuffer: ByteArray,
    ) = ClassPool.getDefault().makeClass(ByteArrayInputStream(classfileBuffer))?.run {
        getDeclaredMethod("run").also {
            it.insertBefore("""
                com.uber.cadence.Memo memo = attributes.getMemo();
                if (memo != null) {
                    java.util.Map fields = memo.getFields();
                    if (fields != null) {
                        java.nio.ByteBuffer byteBuffer = (java.nio.ByteBuffer) fields.get("$cadenceHeaders"); 
                        if (byteBuffer != null) {
                            final byte[] valueBytes = new byte[byteBuffer.remaining()];
                            byteBuffer.mark(); 
                            byteBuffer.get(valueBytes); 
                            byteBuffer.reset();
                            String base64 = (String) com.uber.cadence.converter.JsonDataConverter.getInstance().fromData(valueBytes,String.class,String.class);
                            ${RequestHolder::class.java.name}.INSTANCE.${RequestHolder::store.name}(java.util.Base64.getDecoder().decode(base64));
                        }
                    }
                }
            """.trimIndent())
        }
        toBytecode()
    }

}
