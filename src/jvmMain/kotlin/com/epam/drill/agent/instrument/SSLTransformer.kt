package com.epam.drill.agent.instrument

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object SSLTransformer {
    private val logger = Logging.logger(Transformer::class.jvmName)

    actual fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray? {
        return try {
            ClassPool.getDefault().appendClassPath(LoaderClassPath(loader as? ClassLoader))
            ClassPool.getDefault().makeClass(ByteArrayInputStream(classfileBuffer))?.run {
                getMethod(
                    "unwrap",
                    "(Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;II)Ljavax/net/ssl/SSLEngineResult;"
                )?.insertAfter(
                    """
                       com.epam.drill.request.HttpRequest.INSTANCE.${HttpRequest::parse.name}($2);
                    """.trimIndent()
                ) ?: run {
                    return null
                }
                return toBytecode()


            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }
}
