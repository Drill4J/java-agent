package com.epam.drill.request

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.http.apache.*
import com.epam.drill.agent.instrument.http.java.*
import com.epam.drill.agent.instrument.http.ok.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import org.objectweb.asm.*

@Kni
actual object HttpClientInstrumentation {

    private val logger = Logging.logger(HttpClientInstrumentation::class.java.name)

    private val strategies: List<TransformStrategy> = listOf(
        OkHttpClient(), ApacheClient(), JavaHttpUrlConnection()
    )

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = ClassReader(classFileBuffer).let { classReader ->
        strategies.firstOrNull {
            it.permit(classReader)
        }?.also {
            logger.debug { "Http hook is off, starting transform ${it::class.java.simpleName} class kClassName $className... } " }
        }?.transform(className, classFileBuffer, loader, protectionDomain)
    }


}

