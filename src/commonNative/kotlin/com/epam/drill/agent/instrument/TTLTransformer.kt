package com.epam.drill.agent.instrument

actual object TTLTransformer {

    actual fun transform(
        loader: Any?,
        classFile: String?,
        classBeingRedefined: Any?,
        classFileBuffer: ByteArray
    ): ByteArray? {
        return TTLTransformerStub.transform(loader, classFile, classBeingRedefined, classFileBuffer)
    }
}
