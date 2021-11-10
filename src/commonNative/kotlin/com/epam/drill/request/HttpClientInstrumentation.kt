package com.epam.drill.request

import org.objectweb.asm.*

// TODO Remove after update kotlin to 1.5
actual object HttpClientInstrumentation {

    fun permit(classReader: ClassReader): Boolean {
        val parentClassName = runCatching { classReader.superName }.getOrDefault("")
        return classReader.interfaces.any { it == "okhttp3/internal/http/HttpCodec" } ||
                classReader.interfaces.any { "org/apache/http/HttpClientConnection" == it } ||
                parentClassName == "java/net/HttpURLConnection" ||
                parentClassName == "javax/net/ssl/HttpsURLConnection"
    }

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? {
        return HttpClientInstrumentationStub.transform(
            className,
            classFileBuffer,
            loader,
            protectionDomain)
    }
}
