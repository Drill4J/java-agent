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
package com.epam.drill.agent.instrument

import kotlin.reflect.KCallable
import javassist.ByteArrayClassPath
import javassist.ClassPool
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers

abstract class AbstractWsMessagesProxyDelegate {

    private val byteBuddy by lazy(::ByteBuddy)
    private val proxyClassCache = TypeCache<String>()

    protected fun createDelegatedGetterProxy(
        className: String,
        delegatedMethod: String,
        delegateMethod: KCallable<*>,
        classPool: ClassPool,
        proxyName: String = "${className}Proxy",
        targetField: String = "target",
        constructorCall: (Class<*>) -> MethodCall
    ): Class<*> = Class.forName(className, true, classPool.classLoader).let { clazz ->
        proxyClassCache.findOrInsert(clazz.classLoader, proxyName) {
            byteBuddy.subclass(clazz)
                .name(proxyName)
                .modifiers(Visibility.PUBLIC)
                .defineField(targetField, clazz, Visibility.PRIVATE)
                .defineConstructor(Visibility.PUBLIC)
                .withParameter(clazz)
                .intercept(constructorCall(clazz)
                    .andThen(FieldAccessor.ofField(targetField).setsArgumentAt(0)))
                .method(ElementMatchers.isPublic<MethodDescription>()
                    .and(ElementMatchers.isDeclaredBy(clazz)))
                .intercept(MethodCall.invokeSelf().onField(targetField).withAllArguments())
                .method(ElementMatchers.named<MethodDescription>(delegatedMethod)
                    .and(ElementMatchers.takesNoArguments()))
                .intercept(MethodDelegation.withDefaultConfiguration()
                    .filter(ElementMatchers.named(delegateMethod.name))
                    .to(this@AbstractWsMessagesProxyDelegate))
                .make()
                .load(clazz.classLoader, ClassLoadingStrategy.Default.INJECTION)
                .also { classPool.appendClassPath(ByteArrayClassPath(proxyName, it.bytes)) }
                .loaded
        }
    }

}
