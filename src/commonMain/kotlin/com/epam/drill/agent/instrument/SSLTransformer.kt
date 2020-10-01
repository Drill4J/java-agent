package com.epam.drill.agent.instrument

expect object SSLTransformer {
     fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray?
}
