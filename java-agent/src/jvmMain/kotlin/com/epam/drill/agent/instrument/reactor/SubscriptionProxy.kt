package com.epam.drill.agent.instrument.reactor

import mu.KotlinLogging
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Method
import java.util.function.BiFunction
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

class SubscriptionProxy(
    private val target: Any,
    private val drillWrapper: BiFunction<Supplier<*>, Any, *>,
    private val drillRequest: Any
) : MethodInterceptor {
    override fun intercept(proxy: Any, method: Method, args: Array<out Any>, superMethod: MethodProxy): Any? {
        val methodName = method.name
        return when (methodName) {
            "request", "cancel" -> {
                drillWrapper.apply(Supplier {
                    logger.info("[${Thread.currentThread().name}] ${target.javaClass.simpleName}.${methodName}:${target.hashCode()}, context $drillRequest")
                    superMethod.invoke(target, args)
                }, drillRequest)
            }

            else -> superMethod.invoke(target, args)
        }
    }
}