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
package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import javassist.CtClass
import mu.KotlinLogging

const val FLUX_CLASS_NAME = "reactor/core/publisher/Flux"
const val MONO_CLASS_NAME = "reactor/core/publisher/Mono"
const val PARALLEL_FLUX_CLASS_NAME = "reactor/core/publisher/ParallelFlux"
const val SCHEDULERS_CLASS_NAME = "reactor/core/scheduler/Schedulers"

private val reactorTransformers = setOf(
    FluxTransformerObject,
    MonoTransformerObject,
    ParallelFluxTransformerObject,
    SchedulersTransformerObject
)

actual object ReactorTransformer : TransformerObject, AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        reactorTransformers.any { it.permit(className, null, null) }

    override fun transform(className: String, ctClass: CtClass) {
        reactorTransformers.find { it.permit(className, null, null) }
            ?.transform(className, ctClass)
            ?: logger.error { "Reactor transformer object for class $className not found" }
    }
}