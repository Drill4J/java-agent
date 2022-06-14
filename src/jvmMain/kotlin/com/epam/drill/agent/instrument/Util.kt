package com.epam.drill.agent.instrument

import com.epam.drill.agent.*
import javassist.*
import java.security.*

inline fun createAndTransform(
    classBytes: ByteArray,
    loader: Any?,
    protectionDomain: Any?,
    additionalPoolFiles: List<String> = AgentConfig.drillInstallationDir()?.let { listOf(it) } ?: emptyList(),
    transformer: (CtClass, ClassPool, ClassLoader?, ProtectionDomain?) -> ByteArray?,
): ByteArray? = com.epam.drill.agent.instrument.util.createAndTransform(
    classBytes,
    loader,
    protectionDomain
) { ctClass, classPool, classLoader, prDomain ->
    additionalPoolFiles.forEach { classPool.appendClassPath(it) }
    transformer(ctClass, classPool, classLoader, prDomain)
}