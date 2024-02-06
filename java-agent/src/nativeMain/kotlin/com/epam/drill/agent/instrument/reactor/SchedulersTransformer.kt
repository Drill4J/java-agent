package com.epam.drill.agent.instrument.reactor

import com.epam.drill.instrument.IStrategy
import com.epam.drill.agent.instrument.jvm.callTransformerTransformMethod
import com.epam.drill.instrument.http.OkHttpClient

actual object SchedulersTransformer: IStrategy {
    actual override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "reactor/core/scheduler/Schedulers"
    }

    actual override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? =
        callTransformerTransformMethod(
            SchedulersTransformer::class,
            SchedulersTransformer::transform,
            className,
            classFileBuffer,
            loader,
            protectionDomain
        )
}