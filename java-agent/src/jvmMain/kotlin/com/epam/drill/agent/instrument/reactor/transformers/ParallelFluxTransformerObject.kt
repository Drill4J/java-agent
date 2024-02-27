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
package com.epam.drill.agent.instrument.reactor.transformers

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.ClassPathProvider
import com.epam.drill.agent.instrument.RuntimeClassPathProvider
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.instrument.reactor.PublisherAssembler
import com.epam.drill.agent.instrument.servers.PARALLEL_FLUX_CLASS_NAME
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

/**
 * Transformer for {@link reactor.core.publisher.ParallelFlux}.
 */
object ParallelFluxTransformerObject: TransformerObject,
    AbstractTransformerObject(),
    ClassPathProvider by RuntimeClassPathProvider {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == PARALLEL_FLUX_CLASS_NAME

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/ParallelFlux;)Lreactor/core/publisher/ParallelFlux;").insertCatching(
            CtBehavior::insertBefore,
            """
              $1 = (reactor.core.publisher.ParallelFlux) ${PublisherAssembler::class.java.name}.${PublisherAssembler::onAssembly.name}($1, reactor.core.publisher.ParallelFlux.class);                    
            """.trimIndent()
        )
    }
}