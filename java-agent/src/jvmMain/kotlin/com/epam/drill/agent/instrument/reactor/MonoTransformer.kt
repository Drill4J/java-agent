package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

actual object MonoTransformer: TransformerObject, AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == "reactor/core/publisher/Mono"

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/Mono;)Lreactor/core/publisher/Mono;").insertCatching(
            CtBehavior::insertBefore,
            """
                    $1 = (reactor.core.publisher.Mono) com.epam.drill.agent.instrument.reactor.PublisherAssembler.onAssembly($1, reactor.core.publisher.Mono.class);
                """.trimIndent()
        )
    }
}