/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.instrument.reactor

import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
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

/**
 * The cache of proxy classes
 */
val proxyClassCache = TypeCache<Class<*>>()

/**
 * Constructs a proxy class and an associated instance for intercepting method calls using the specified interceptor.
 * Each public method of the class will be intercepted by the provided interceptor.
 * @param delegate the original instance of the class.
 * @param clazz the class that will be delegated. Must be a superclass of the class of the delegate.
 * @param interceptor the Byte buddy method interceptor.
 * @param configure the Byte buddy configuration which will be applied before building proxy class.
 * @param initialize the proxy instance initialization logic.
 * @return the proxy instance.
 */
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

/**
 * Propagates the drill request to the given "body" lambda expression via the ThreadLocal context.
 * If the request was already propagated, the previous request will be restored after the body invocation.
 * @param ctx the drill request
 * @param body the body function in which the drill request will be propagated
 * @return the result of the body function
 */
inline fun <T> propagateDrillRequest(ctx: DrillRequest, requestHolder: RequestHolder, body: () -> T?): T? {
    val previous = requestHolder.retrieve()
    if (previous != ctx) {
        requestHolder.store(ctx)
    }
    try {
        return body()
    } finally {
        if (previous != ctx && previous != null) {
            // If previous context is different from current one - restore previous
            requestHolder.store(previous)
        } else if (previous == null) {
            // If no previous context available - just cleanup the current one
            requestHolder.remove()
        }
    }
}