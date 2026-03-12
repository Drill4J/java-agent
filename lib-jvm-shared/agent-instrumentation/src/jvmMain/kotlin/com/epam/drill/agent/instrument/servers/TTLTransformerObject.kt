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
package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.common.configuration.AgentConfiguration
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.instrument.AbstractPropagationTransformer
import com.epam.drill.agent.ttl.threadpool.agent.TtlAgent
import com.epam.drill.agent.ttl.threadpool.agent.internal.logging.Logger
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.ClassInfo
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.JavassistTransformlet
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.TtlExecutorTransformlet
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.TtlForkJoinTransformlet
import com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.TtlTimerTaskTransformlet
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_TTL_ENABLED

abstract class TTLTransformerObject(agentConfiguration: AgentConfiguration) : AbstractPropagationTransformer(agentConfiguration) {

    private val directTtlClasses = listOf(
        "java/util/concurrent/ScheduledThreadPoolExecutor",
        "java/util/concurrent/ThreadPoolExecutor",
        "java/util/concurrent/ForkJoinTask",
        "java/util/concurrent/ForkJoinPool"
    )
    private val threadPoolExecutorClass = "java/util/concurrent/ThreadPoolExecutor"
    private val timerTaskClass = "java/util/TimerTask"
    private val runnableInterface = "java/lang/Runnable"
    private val jdkInternal = "jdk/internal"

    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

    override val logger = KotlinLogging.logger {}

    init {
        Logger.setLoggerImplType("")
        transformletList.add(TtlExecutorTransformlet(false))
        transformletList.add(TtlForkJoinTransformlet(false))
        if (TtlAgent.isEnableTimerTask()) transformletList.add(TtlTimerTaskTransformlet())
    }

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_TTL_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        if (directTtlClasses.contains(className)) return true
        return (threadPoolExecutorClass == superName || interfaces.contains(runnableInterface))
                && className != timerTaskClass && !className.startsWith(jdkInternal)
    }

    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ) = try {
        val classInfo = ClassInfo(className.replace('/', '.'), classFileBuffer, (loader as? ClassLoader))
        transformletList.firstOrNull { classInfo.also(it::doTransform).isModified }
            ?.let { classInfo.ctClass.toBytecode() }
            ?: classFileBuffer
    } catch (e: Exception) {
        logger.error(e) { "transform: Failed to transform class $className" }
        classFileBuffer
    }

    override fun transform(className: String, ctClass: CtClass): Unit = throw NotImplementedError()

}
