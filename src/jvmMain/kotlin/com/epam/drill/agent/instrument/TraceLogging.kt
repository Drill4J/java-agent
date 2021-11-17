package com.epam.drill.agent.instrument

import com.epam.drill.agent.instrument.util.*
import com.epam.drill.kni.*
import com.epam.drill.logging.*
import javassist.*

@Kni
actual object TraceLogging {

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = createAndTransform(classFileBuffer, loader, protectionDomain) { ctClass, _, _, _ ->
        ctClass.declaredMethods.forEach { method ->
            if (!Modifier.isNative(method.modifiers) && !method.isEmpty) {
                method.parameterTypes
                method.insertBefore("""
                ${TraceLog::class.java.name}.INSTANCE.${TraceLog::log.name}("${ctClass.simpleName}","${method.name}", "START");
            """.trimIndent())

                method.insertAfter("""
                ${TraceLog::class.java.name}.INSTANCE.${TraceLog::log.name}("${ctClass.simpleName}","${method.name}", "FINISH");
            """.trimIndent())
            }
        }
        ctClass.toBytecode()
    }
}
