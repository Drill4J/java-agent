package com.epam.drill.agent.instrument.reactor

import com.epam.drill.instrument.IStrategy
import com.epam.drill.instrument.TransformStrategy
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import mu.KotlinLogging
import java.lang.reflect.Modifier
import java.security.ProtectionDomain

actual object CoreSubscriberTransformer: TransformStrategy(), IStrategy {
    private val logger = KotlinLogging.logger {}

    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/CoreSubscriber"
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
//        runCatching {
//            val drillRequest = ClassPool.getDefault().get("java.lang.Object")
//            val field = CtField(drillRequest, "_drillRequest", ctClass)
//            field.modifiers = Modifier.PUBLIC
//            ctClass.addField(field)
//
//            ctClass.getDeclaredMethod("onSubscribe").insertBefore(
//                """
//               com.epam.drill.agent.instrument.error.InstrumentationErrorLogger.INSTANCE.info("onSubscribe");
//            """.trimIndent()
//            )
//
//            wrapRequest(ctClass.getDeclaredMethod("onNext"))
//            wrapRequest(ctClass.getDeclaredMethod("onError"))
//            wrapRequest(ctClass.getDeclaredMethod("onComplete"))
//        }.onFailure {
//            logger.warn(it) { "Instrumentation error. Reason:" }
//        }
        return ctClass.toBytecode()
    }

    private fun wrapRequest(method: CtMethod) {
        method.insertBefore(
            """             
               if (_drillRequest != null) {
                   com.epam.drill.agent.request.RequestHolder.INSTANCE.storeRequest((com.epam.drill.agent.request.DrillRequest)_drillRequest);
               }                                                                                           
            """.trimIndent()
        )
        method.insertAfter(
            """
               com.epam.drill.agent.request.RequestHolder.INSTANCE.removeRequest();
            """.trimIndent(), true
        )
    }
}