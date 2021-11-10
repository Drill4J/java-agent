package com.epam.drill.request

expect object HttpClientInstrumentation {

    fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray?
}
