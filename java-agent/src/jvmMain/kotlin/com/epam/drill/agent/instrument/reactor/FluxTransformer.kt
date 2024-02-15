package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.TransformerObject
import javassist.CtBehavior
import javassist.CtClass
import mu.KotlinLogging

actual object FluxTransformer: TransformerObject, AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == "reactor/core/publisher/Flux"

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/Flux;)Lreactor/core/publisher/Flux;").insertCatching(
            CtBehavior::insertBefore,
            """
                $1 = (reactor.core.publisher.Flux) com.epam.drill.agent.instrument.reactor.PublisherAssembler.onAssembly($1, reactor.core.publisher.Flux.class);
            """.trimIndent()
        )
        ctClass.getMethod("onAssembly", "(Lreactor/core/publisher/ConnectableFlux;)Lreactor/core/publisher/ConnectableFlux;").insertCatching(
            CtBehavior::insertBefore,
            """
                $1 = (reactor.core.publisher.ConnectableFlux) com.epam.drill.agent.instrument.reactor.PublisherAssembler.onAssembly($1, reactor.core.publisher.ConnectableFlux.class);                    
            """.trimIndent()
        )
    }
}