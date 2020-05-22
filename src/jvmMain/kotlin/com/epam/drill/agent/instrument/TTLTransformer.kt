@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.threadpool.agent.*
import com.alibaba.ttl.threadpool.agent.internal.logging.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.*
import mu.*
import java.lang.instrument.*
import java.security.*
import java.util.*
import kotlin.reflect.jvm.*


actual object TTLTransformer : ClassFileTransformer {
    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

    private val logger = KotlinLogging.logger(TTLTransformer::class.jvmName)

    init {
        Logger.setLoggerImplType("")
        transformletList.add(TtlExecutorTransformlet(false))
        transformletList.add(TtlForkJoinTransformlet(false))
        if (TtlAgent.isEnableTimerTask())
            transformletList.add(TtlTimerTaskTransformlet())
    }

    override fun transform(
        loader: ClassLoader?,
        classFile: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classFileBuffer: ByteArray
    ): ByteArray? {
        try {
            // Lambda has no class file, no need to transform, just return.
            if (classFile == null) return null
            val className = toClassName(classFile)
            val classInfo =
                ClassInfo(className, classFileBuffer, loader)
            for (transformlet in transformletList) {
                transformlet.doTransform(classInfo)
                if (classInfo.isModified) return classInfo.ctClass.toBytecode()
            }
        } catch (t: Throwable) {
            logger.warn(t) { "Fail to transform class $classFile" }
        }
        return null
    }

    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    private fun toClassName(classFile: String): String {
        return classFile.replace('/', '.')
    }
}

