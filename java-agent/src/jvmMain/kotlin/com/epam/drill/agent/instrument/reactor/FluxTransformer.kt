package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.error.wrapCatching
import com.epam.drill.instrument.IStrategy
import com.epam.drill.instrument.TransformStrategy
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import java.security.ProtectionDomain

actual object FluxTransformer: TransformStrategy(), IStrategy {
    private val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/publisher/Flux"
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
            ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/Flux;)Lreactor/core/publisher/Flux;").wrapCatching(
                CtMethod::insertBefore,
                """
                    com.epam.drill.agent.instrument.reactor.DrillWrapper drillWrapper = com.epam.drill.agent.instrument.reactor.DrillWrapper.INSTANCE;
                    com.epam.drill.agent.instrument.reactor.DrillSupplier drillSupplier = com.epam.drill.agent.instrument.reactor.DrillSupplier.INSTANCE;
                    $1 = (reactor.core.publisher.Flux) com.epam.drill.agent.instrument.reactor.PublisherProxy.onAssembly(
                        $1,
                        drillWrapper,
                        drillSupplier,
                        reactor.core.publisher.Flux.class,
                        reactor.core.CoreSubscriber.class,
                        org.reactivestreams.Subscription.class);
                """.trimIndent()
            )
            ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/ConnectableFlux;)Lreactor/core/publisher/ConnectableFlux;").wrapCatching(
                CtMethod::insertBefore,
                """
                    com.epam.drill.agent.instrument.reactor.DrillWrapper drillWrapper = com.epam.drill.agent.instrument.reactor.DrillWrapper.INSTANCE;
                    com.epam.drill.agent.instrument.reactor.DrillSupplier drillSupplier = com.epam.drill.agent.instrument.reactor.DrillSupplier.INSTANCE;
                    $1 = (reactor.core.publisher.ConnectableFlux) com.epam.drill.agent.instrument.reactor.PublisherProxy.onAssembly(
                        $1,
                        drillWrapper,
                        drillSupplier,
                        reactor.core.publisher.ConnectableFlux.class,
                        reactor.core.CoreSubscriber.class,
                        org.reactivestreams.Subscription.class);
                """.trimIndent()
            )
        }.onFailure {
            logger.warn(it) { "Instrumentation error. Reason:" }
        }
        return ctClass.toBytecode()
    }
}