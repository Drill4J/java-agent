package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.request.DrillRequest
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtMethod
import mu.KotlinLogging

actual object SchedulersTransformer: TransformerObject, AbstractTransformerObject() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) =
        className == "reactor/core/scheduler/Schedulers"

    override fun transform(className: String, ctClass: CtClass) {
        ctClass.getDeclaredMethod("onSchedule").insertCatching(
            CtBehavior::insertBefore,
                """
                    ${DrillRequest::class.java.name} drillRequest = ${RequestHolder::class.java.name}.INSTANCE.${RequestHolder::retrieve.name}();
                    if (drillRequest != null)
                        $1 = new ${PropagatedDrillContextRunnable::class.java.name}(drillRequest, $1);                    
                """.trimIndent()
            )
    }
}