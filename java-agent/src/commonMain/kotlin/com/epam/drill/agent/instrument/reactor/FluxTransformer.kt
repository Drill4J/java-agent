package com.epam.drill.agent.instrument.reactor

import com.epam.drill.instrument.IStrategy

expect object FluxTransformer: IStrategy {
    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray?

    override fun permit(
        className: String?,
        superName: String?,
        interfaces: Array<String?>
    ): Boolean
}