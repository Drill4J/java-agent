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

abstract class AbstractTransformer {

    abstract fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray?

    abstract fun logError(
        exception: Throwable,
        message: String
    )

    protected fun CtMethod.insertCatching(insert: CtMethod.(String) -> Unit, code: String) = try {
        insert(
            """
                try {
                    $code
                } catch (Throwable e) {
                    ${this@AbstractTransformer::class.java.name}.INSTANCE.${this@AbstractTransformer::logError.name}(e, "Error in the injected code, method name: $name.");
                }
            """.trimIndent()
        )
    } catch (e: Throwable) {
        logError(e, "insertCatching: Can't insert code, method name: $name")
    }

}
