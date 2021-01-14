package com.epam.drill.agent.instrument

import com.epam.drill.core.transport.*

actual object TomcatTransformer {
    private val idHeaderPair = idHeaderPairFromConfig()

    actual fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
    ): ByteArray? = TomcatTransformerStub.transform(className, classFileBuffer, loader)

    actual fun retrieveAdminAddress(): ByteArray = retrieveAdminUrl().encodeToByteArray()

    actual fun idHeaderConfigKey(): ByteArray = idHeaderPair.first.encodeToByteArray()

    actual fun idHeaderConfigValue(): ByteArray = idHeaderPair.second.encodeToByteArray()

}
