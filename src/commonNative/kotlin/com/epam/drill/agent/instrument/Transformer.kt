package com.epam.drill.agent.instrument

actual object Transformer {
    const val servletListener = "javax/servlet/ServletContextListener"

    actual fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray? {
        return TransformerStub.transform(className, classfileBuffer, loader)
    }
}
