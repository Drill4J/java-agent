package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.error.wrapCatching
import com.epam.drill.instrument.IStrategy
import com.epam.drill.instrument.TransformStrategy
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import java.security.ProtectionDomain

actual object ParallelFluxTransformer: TransformStrategy(), IStrategy {
    private val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/publisher/ParallelFlux"
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? {
        return super.transform(className, classFileBuffer, loader, protectionDomain)
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        runCatching {
            ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/ParallelFlux;)Lreactor/core/publisher/ParallelFlux;").wrapCatching(
                CtMethod::insertBefore,
                """
                    $1 = (reactor.core.publisher.ParallelFlux) com.epam.drill.agent.instrument.reactor.PublisherAssembler.onAssembly($1, reactor.core.publisher.ParallelFlux.class);                    
                """.trimIndent()
            )
        }.onFailure {
            logger.warn(it) { "Instrumentation error. Reason:" }
        }
        return ctClass.toBytecode()
    }
}