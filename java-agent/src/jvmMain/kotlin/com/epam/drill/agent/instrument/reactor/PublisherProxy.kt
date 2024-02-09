package com.epam.drill.agent.instrument.reactor

import mu.KotlinLogging
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Method
import java.util.function.BiFunction
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

class PublisherProxy(
    private val target: Any,
    private val drillWrapper: BiFunction<Supplier<*>, Any, *>,
    private val drillRequest: Any?,
    private val subscriberClass: Class<*>,
    private val subscriptionClass: Class<*>,
) : MethodInterceptor {

    override fun intercept(proxy: Any, method: Method, args: Array<out Any>, superMethod: MethodProxy): Any? {
        val methodName = method.name
        return when (methodName) {
            "subscribe" -> {
                val subscriber = args[0]
                val currentContextMethod = subscriber.javaClass.getMethod("currentContext")
                currentContextMethod.isAccessible = true
                val context = currentContextMethod.invoke(subscriber)

                val getOrDefaultMethod = context.javaClass.getMethod("getOrDefault", Any::class.java, Any::class.java)
                getOrDefaultMethod.isAccessible = true
                val contextualDrillRequest = getOrDefaultMethod.invoke(context, "DrillRequest", null)

                logger.info("[${Thread.currentThread().name}] ${target.javaClass.simpleName}.${methodName}:${target.hashCode()}, context $contextualDrillRequest, drillRequest $drillRequest")
                val parentDrillRequest =
                    contextualDrillRequest ?: drillRequest ?: return superMethod.invoke(target, args)

                val newContext = if (drillRequest != contextualDrillRequest) {
                    val putMethod = context.javaClass.getMethod("put", Any::class.java, Any::class.java)
                    putMethod.isAccessible = true
                    putMethod.invoke(context, "DrillRequest", drillRequest)
                } else context

                val enhancer = Enhancer()
                enhancer.setSuperclass(subscriberClass)
                enhancer.setCallback(
                    SubscriberProxy(
                        subscriber,
                        newContext,
                        drillWrapper,
                        parentDrillRequest,
                        subscriptionClass
                    )
                )
                val subscriberProxy = enhancer.create()
                drillWrapper.apply(Supplier {
                    logger.info("[" + Thread.currentThread().name + "] ${target.javaClass.simpleName}.${methodName}:${target.hashCode()}, drillRequest $drillRequest")
                    superMethod.invoke(target, arrayOf(subscriberProxy))
                }, parentDrillRequest)
            }

            else -> superMethod.invoke(target, args)
        }
    }

    companion object {
        @JvmStatic
        fun <T> onAssembly(
            target: Any,
            drillWrapper: BiFunction<Supplier<*>, Any, *>,
            drillSupplier: Supplier<Any?>,
            publisherClass: Class<*>,
            subscriberClass: Class<*>,
            subscriptionClass: Class<*>
        ): Any {
            val drillRequest = drillSupplier.get()
            logger.info("[${Thread.currentThread().name}] ${target.javaClass.simpleName}.onAssembly:${target.hashCode()}, drillRequest $drillRequest")
            val enhancer = Enhancer()
            enhancer.setSuperclass(publisherClass)
            enhancer.setCallback(PublisherProxy(target, drillWrapper, drillRequest, subscriberClass, subscriptionClass))
            return enhancer.create()
        }
    }
}
