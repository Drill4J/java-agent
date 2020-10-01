package com.epam.drill.agent.instrument

import com.alibaba.ttl.internal.javassist.ClassPool
import com.alibaba.ttl.internal.javassist.LoaderClassPath
import com.epam.drill.kni.Kni
import com.epam.drill.logger.Logging
import com.epam.drill.request.*
import java.io.ByteArrayInputStream
import kotlin.reflect.jvm.jvmName

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
