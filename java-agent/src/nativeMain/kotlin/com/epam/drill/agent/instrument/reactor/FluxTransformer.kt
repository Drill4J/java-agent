package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.jvm.callTransformerTransformMethod
import com.epam.drill.instrument.IStrategy

actual object FluxTransformer: IStrategy {
    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/publisher/Flux"
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? =
        callTransformerTransformMethod(
            FluxTransformer::class,
            FluxTransformer::transform,
            className,
            classFileBuffer,
            loader,
            protectionDomain
        )
}