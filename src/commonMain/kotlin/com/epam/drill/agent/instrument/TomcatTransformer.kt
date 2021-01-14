package com.epam.drill.agent.instrument

expect object TomcatTransformer {
    fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
    ): ByteArray?

    fun retrieveAdminAddress(): ByteArray
    fun idHeaderConfigKey(): ByteArray
    fun idHeaderConfigValue(): ByteArray
}
