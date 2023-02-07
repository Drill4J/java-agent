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

import com.alibaba.ttl.threadpool.agent.*
import com.alibaba.ttl.threadpool.agent.internal.logging.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import java.util.*
import kotlin.reflect.jvm.*

@Kni
actual object TTLTransformer {
    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

    private val logger = Logging.logger(TTLTransformer::class.jvmName)

    init {
        Logger.setLoggerImplType("")
        transformletList.add(TtlExecutorTransformlet(false))
        transformletList.add(TtlForkJoinTransformlet(false))
        if (TtlAgent.isEnableTimerTask())
            transformletList.add(TtlTimerTaskTransformlet())
    }

    actual fun transform(
        loader: Any?,
        classFile: String?,
        classBeingRedefined: Any?,
        classFileBuffer: ByteArray
    ): ByteArray? {
        try {
            // Lambda has no class file, no need to transform, just return.
            if (classFile == null) return null
            val className = toClassName(classFile)
            val classInfo = ClassInfo(className, classFileBuffer, (loader as? ClassLoader))
            for (transformlet in transformletList) {
                transformlet.doTransform(classInfo)
                if (classInfo.isModified) return classInfo.ctClass.toBytecode()
            }
        } catch (e: Exception) {
            logger.warn(e) { "Fail to transform class $classFile" }
        }
        return null
    }

    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    private fun toClassName(classFile: String): String {
        return classFile.replace('/', '.')
    }
}

