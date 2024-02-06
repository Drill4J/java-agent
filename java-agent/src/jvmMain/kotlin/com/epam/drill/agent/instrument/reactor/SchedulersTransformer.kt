package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.error.wrapCatching
import com.epam.drill.instrument.IStrategy
import com.epam.drill.instrument.TransformStrategy
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging
import java.security.ProtectionDomain

actual object SchedulersTransformer : TransformStrategy(), IStrategy {
    private val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/scheduler/Schedulers"
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
            val onScheduleMethod = ctClass.getDeclaredMethod("onSchedule")
            onScheduleMethod.wrapCatching(
                CtMethod::insertBefore,
                """
                    com.epam.drill.agent.request.DrillRequest drillRequest = com.epam.drill.agent.request.RequestHolder.INSTANCE.getRequest();
                    if (drillRequest != null) {
                        com.epam.drill.agent.instrument.error.InstrumentationErrorLogger.INSTANCE.info("task decorated: " + drillRequest);
                        $1 = new com.epam.drill.agent.instrument.reactor.PropagatedDrillContextRunnable(drillRequest, $1);
                    } else {
                        com.epam.drill.agent.instrument.error.InstrumentationErrorLogger.INSTANCE.info("task not decorated");
                    }
                """.trimIndent()
            )
        }.onFailure {
            logger.warn(it) { "Instrumentation error. Reason:" }
        }
        return ctClass.toBytecode()
    }
}