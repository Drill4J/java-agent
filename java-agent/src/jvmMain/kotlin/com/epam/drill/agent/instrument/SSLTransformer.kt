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

import com.epam.drill.agent.instrument.util.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import javassist.*
import kotlin.reflect.jvm.*

@Kni
actual object SSLTransformer {
    private val logger = Logging.logger(SSLTransformer::class.jvmName)

    actual fun transform(
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
                )?.wrapCatching(
                    CtMethod::insertAfter,
                    """
                       com.epam.drill.request.HttpRequest.INSTANCE.${HttpRequest::parse.name}($2);
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
}
