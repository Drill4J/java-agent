package com.epam.drill.agent.instrument.reactor

import mu.KotlinLogging
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Method
import java.util.function.BiFunction
import java.util.function.Supplier


private val logger = KotlinLogging.logger {}

class SubscriberProxy(
    private val target: Any,
    private val context: Any,
    private val drillWrapper: BiFunction<Supplier<*>, Any, *>,
    private val drillRequest: Any,
    private val subscriptionClass: Class<*>
) : MethodInterceptor {
    override fun intercept(proxy: Any, method: Method, args: Array<out Any>, superMethod: MethodProxy): Any? {
        val methodName = method.name
        return when (methodName) {
            "onComplete", "onError", "onNext" -> {
                drillWrapper.apply(Supplier {
                    logger.info("[" + Thread.currentThread().name + "] ${target.javaClass.simpleName}.${methodName}:${target.hashCode()}, context $drillRequest")
                    superMethod.invoke(target, args)
                }, drillRequest)
            }

            "onSubscribe" -> {
                val enhancer = Enhancer()
                enhancer.setSuperclass(subscriptionClass)
                enhancer.setCallback(SubscriptionProxy(args[0], drillWrapper, drillRequest))
                val subscriptionProxy = enhancer.create()


                drillWrapper.apply(Supplier {
                    logger.info("[" + Thread.currentThread().name + "] ${target.javaClass.simpleName}.${methodName}:${target.hashCode()}, context $drillRequest")
                    superMethod.invoke(target, arrayOf(subscriptionProxy))
                }, drillRequest)
            }

            "currentContext" -> {
                return context
            }

            else -> superMethod.invoke(target, args)
        }
    }
}