package com.epam.drill.request

import org.objectweb.asm.*

actual object HttpClientInstrumentation {
    actual fun initCallbacks() {
        return HttpClientInstrumentationStub.initCallbacks()
    }

    actual fun permitAndTransform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? {
        return HttpClientInstrumentationStub.permitAndTransform(
            className,
            classFileBuffer,
            loader,
            protectionDomain)
    }
}
