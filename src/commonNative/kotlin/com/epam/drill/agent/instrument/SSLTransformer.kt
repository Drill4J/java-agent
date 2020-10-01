package com.epam.drill.agent.instrument

actual object SSLTransformer {
    const val SSL_ENGINE_CLASS_NAME = "javax/net/ssl/SSLEngine"

    actual fun transform(
        className: String,
        classfileBuffer: ByteArray,
        loader: Any?
    ): ByteArray? {
        return SSLTransformerStub.transform(className, classfileBuffer, loader)
    }
}
