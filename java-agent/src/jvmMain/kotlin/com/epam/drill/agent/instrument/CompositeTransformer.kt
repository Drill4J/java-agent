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

import mu.KotlinLogging
import org.objectweb.asm.ClassReader

actual object CompositeTransformer : Transformer {
    private val logger = KotlinLogging.logger {}
    private val transformers = TransformerRegistrar

    override fun precheck(
        className: String,
        loader: Any?,
        protectionDomain: Any?
    ): Boolean {
        return transformers.enabledTransformers.any { it.precheck(className, loader, protectionDomain) }
    }

    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray {
        val reader = runCatching { ClassReader(classFileBuffer) }
            .onFailure { logger.warn(it) { "Can't read class: $classFileBuffer" } }
            .getOrNull() ?: return classFileBuffer
        val enabledTransformers = runCatching { transformers.enabledTransformers }.onFailure {
            logger.warn(it) { "Can't get enabled transformers for class: $className" }
        }.getOrNull() ?: return classFileBuffer
        return enabledTransformers.fold(classFileBuffer) { bytes, transformer ->
            runCatching {
                when {
                    transformer is TransformerObject && !transformer.permit(
                        className,
                        reader.superName,
                        reader.interfaces
                    ) -> bytes

                    else -> transformer.transform(className, bytes, loader, protectionDomain)
                }
            }.onFailure {
                logger.warn(it) { "Can't transform class: $className with ${transformer::class.simpleName}" }
            }.getOrNull()?.takeIf { it !== bytes }?.also {
                logger.debug { "$className was transformed by ${transformer::class.simpleName}" }
            } ?: bytes
        }
    }
}