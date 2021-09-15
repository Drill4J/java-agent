package com.epam.drill.agent.instrument

import com.alibaba.ttl.threadpool.agent.internal.javassist.*
import com.epam.drill.logger.*

inline fun CtMethod.wrapCatching(
    insert: CtMethod.(String) -> Unit,
    code: String,
) {
    runCatching {
        insert(
            """
            try {
                $code
            } catch (Throwable e) {
                ${InstrumentationErrorLogger::class.java.name}.INSTANCE.${InstrumentationErrorLogger::error.name}(e, "Error in the injected code. Method name: $name.");
            }
        """.trimIndent()
        )
    }.onFailure { InstrumentationErrorLogger.warn(it, "Can't insert code. Method name: $name") }
}

object InstrumentationErrorLogger {
    private val logger = Logging.logger("instrumentation")

    fun error(exception: Throwable, message: String) {
        logger.error(exception) { message }
    }

    fun warn(exception: Throwable, message: String) {
        logger.warn(exception) { message }
    }
}
