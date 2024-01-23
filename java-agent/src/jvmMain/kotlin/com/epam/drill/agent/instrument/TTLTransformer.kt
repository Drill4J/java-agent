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
@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.threadpool.agent.TtlAgent
import com.alibaba.ttl.threadpool.agent.internal.logging.Logger
import com.alibaba.ttl.threadpool.agent.internal.transformlet.ClassInfo
import com.alibaba.ttl.threadpool.agent.internal.transformlet.JavassistTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlExecutorTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlForkJoinTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlTimerTaskTransformlet
import mu.KotlinLogging

actual object TTLTransformer : AbstractTransformerObject() {
    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

    override val logger = KotlinLogging.logger {}

    init {
        Logger.setLoggerImplType("")
        transformletList.add(TtlExecutorTransformlet(false))
        transformletList.add(TtlForkJoinTransformlet(false))
        if (TtlAgent.isEnableTimerTask())
            transformletList.add(TtlTimerTaskTransformlet())
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray? {
        try {
            val classInfo = ClassInfo(toClassName(className), classFileBuffer, (loader as? ClassLoader))
            for (transformlet in transformletList) {
                transformlet.doTransform(classInfo)
                if (classInfo.isModified) return classInfo.ctClass.toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Fail to transform class $className" }
        }
        return null
    }

    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    private fun toClassName(classFile: String): String {
        return classFile.replace('/', '.')
    }

}

