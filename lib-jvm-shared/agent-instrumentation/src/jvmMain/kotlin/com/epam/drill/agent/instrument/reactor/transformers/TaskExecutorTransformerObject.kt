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

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.reactor.PropagatedDrillRequestCallable
import com.epam.drill.agent.instrument.reactor.PropagatedDrillRequestRunnable
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder

abstract class TaskExecutorTransformerObject(agentConfiguration: AgentConfiguration) :
    AbstractReactorTransformerObject(agentConfiguration) {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>) =
        listOf(
            "org/springframework/core/task/SimpleAsyncTaskExecutor",
            "org/springframework/scheduling/concurrent/ConcurrentTaskExecutor",
            "org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor"
        ).contains(className)

    override fun transform(className: String, ctClass: CtClass) {
        logger.info { "transform: Starting TaskExecutorTransformer for $className..." }
        ctClass.getMethod(
            "submitListenable",
            "(Ljava/util/concurrent/Callable;)Lorg/springframework/util/concurrent/ListenableFuture;"
        )
            .insertCatching(
                CtBehavior::insertBefore,
                """
                ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                if (drillRequest != null) $1 = new ${PropagatedDrillRequestCallable::class.java.name}(drillRequest, ${this::class.java.name}.INSTANCE, $1);
                """.trimIndent()
            )
        ctClass.getMethod(
            "submitListenable",
            "(Ljava/lang/Runnable;)Lorg/springframework/util/concurrent/ListenableFuture;"
        )
            .insertCatching(
                CtBehavior::insertBefore,
                """
                ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                if (drillRequest != null) $1 = new ${PropagatedDrillRequestRunnable::class.java.name}(drillRequest, ${this::class.java.name}.INSTANCE, $1);
                """.trimIndent()
            )
        ctClass.getMethod("execute", "(Ljava/lang/Runnable;)V")
            .insertCatching(
                CtBehavior::insertBefore,
                """
                ${DrillRequest::class.java.name} drillRequest = ${this::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                if (drillRequest != null) $1 = new ${PropagatedDrillRequestRunnable::class.java.name}(drillRequest, ${this::class.java.name}.INSTANCE, $1);
                """.trimIndent()
            )
    }

}
