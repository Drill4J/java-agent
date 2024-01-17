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

import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.request.HttpRequest
import com.epam.drill.instrument.util.createAndTransform

actual object SSLTransformer : AbstractTransformer() {

    private val logger = KotlinLogging.logger {}

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        return try {
            ctClass.run {
                getMethod(
                    "unwrap",
                    "(Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;II)Ljavax/net/ssl/SSLEngineResult;"
                )?.insertCatching(
                    CtMethod::insertAfter,
                    """
                       ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::parse.name}($2);
                    """.trimIndent()
                ) ?: run {
                    return null
                }
                return toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }

    override fun logError(exception: Throwable, message: String) = logger.error(exception) { message }

}
