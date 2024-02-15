package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

actual object ParallelFluxTransformer: TransformerObject, AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == "reactor/core/publisher/ParallelFlux"

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/ParallelFlux;)Lreactor/core/publisher/ParallelFlux;").insertCatching(
            CtBehavior::insertBefore,
            """
              $1 = (reactor.core.publisher.ParallelFlux) com.epam.drill.agent.instrument.reactor.PublisherAssembler.onAssembly($1, reactor.core.publisher.ParallelFlux.class);                    
            """.trimIndent()
        )
    }
}