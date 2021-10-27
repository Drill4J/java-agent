package com.epam.drill.request

expect object HttpClientInstrumentation {
    fun initCallbacks()

    fun permitAndTransform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray?
}
