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
import javassist.CtMethod
import mu.KotlinLogging
import com.epam.drill.agent.instrument.request.HttpRequest

actual object SSLTransformer : TransformerObject, AbstractTransformerObject() {

    override val logger = KotlinLogging.logger {}

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("unwrap","(Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;II)Ljavax/net/ssl/SSLEngineResult;")
            .insertCatching(
                CtMethod::insertAfter,
                """
                   ${HttpRequest::class.java.name}.INSTANCE.${HttpRequest::parse.name}($2);
                """.trimIndent()
            )
    }

}
