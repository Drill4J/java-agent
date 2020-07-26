package com.epam.drill.agent.instrument

expect object Transformer {
     fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray?
}
