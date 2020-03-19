@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.threadpool.agent.*
import com.alibaba.ttl.threadpool.agent.internal.logging.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.*
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.*
import java.lang.instrument.*
import java.security.*
import java.util.*


object TTLTransformer : ClassFileTransformer {
    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

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
            println("Fail to transform class $classFile, cause: $t")
        }
        return null
    }

    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    private fun toClassName(classFile: String): String {
        return classFile.replace('/', '.')
    }
}

