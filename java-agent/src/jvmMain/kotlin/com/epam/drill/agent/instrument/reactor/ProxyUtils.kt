package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.request.RequestHolder
import com.epam.drill.common.agent.request.DrillRequest
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.*
import net.bytebuddy.matcher.ElementMatchers.*
import java.util.function.Function

const val DRILL_DELEGATE_FIELD = "drillDelegate"
const val DRILL_REQUEST_FIELD = "drillRequest"
const val DRILL_CONTEXT_FIELD = "drillContext"
const val DRILL_CONTEXT_KEY = "drillRequest"
const val SUBSCRIPTION_CLASS = "org.reactivestreams.Subscription"
const val SUBSCRIBER_CLASS = "reactor.core.CoreSubscriber"


val proxyClassCache = TypeCache<Class<*>>()

@Suppress("UNCHECKED_CAST")
inline fun <T> createProxyDelegate(
    delegate: Any,
    clazz: Class<T>,
    interceptor: Any,
    crossinline configure: DynamicType.Builder.FieldDefinition.Optional.Valuable<*>.() -> DynamicType.Builder.FieldDefinition.Optional.Valuable<*> = { this },
    crossinline initialize: (T, Class<T>) -> Unit = { _, _ -> }
): T {
    val proxyType = proxyClassCache.findOrInsert(clazz.classLoader, clazz) {
        ByteBuddy()
            .subclass(clazz)
            .defineField(DRILL_DELEGATE_FIELD, clazz, Visibility.PUBLIC)
            .let(configure)
            .method(isPublic())
            .intercept(MethodDelegation.withDefaultConfiguration()
                .withBinders(Pipe.Binder.install(Function::class.java))
                .to(interceptor))
            .make()
            .load(clazz.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded
    } as Class<T>
    val proxy = proxyType.getConstructor().newInstance()
    proxyType.getField(DRILL_DELEGATE_FIELD).set(proxy, delegate)
    initialize(proxy, proxyType)
    return proxy
}

inline fun <T> propagateDrillRequest(ctx: DrillRequest, body: () -> T?): T? {
    val previous = RequestHolder.retrieve()
    if (previous != null)
        RequestHolder.remove()
    try {
        RequestHolder.store(ctx)
        return body()
    } finally {
        RequestHolder.remove()
        if (previous != null)
            RequestHolder.store(previous)
    }
}