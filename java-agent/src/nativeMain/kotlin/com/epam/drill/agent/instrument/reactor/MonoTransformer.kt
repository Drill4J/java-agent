package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.jvm.callTransformerTransformMethod
import com.epam.drill.instrument.IStrategy

actual object MonoTransformer: IStrategy {
    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/publisher/Mono"
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? =
        callTransformerTransformMethod(
            MonoTransformer::class,
            MonoTransformer::transform,
            className,
            classFileBuffer,
            loader,
            protectionDomain
        )
}