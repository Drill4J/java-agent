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
package com.epam.drill.agent.instrument.undertow

import java.lang.reflect.Method
import java.nio.ByteBuffer
import javassist.ClassPool
import mu.KotlinLogging
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bind.annotation.FieldValue
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import com.epam.drill.agent.instrument.AbstractWsMessagesProxyDelegate
import com.epam.drill.agent.instrument.PayloadProcessor

class UndertowWsMessagesProxyDelegate(
    private val payloadProcessor: PayloadProcessor
) : AbstractWsMessagesProxyDelegate() {

    private val logger = KotlinLogging.logger {}
    private var pooledProxyClass: Class<*>? = null
    private var textMessageProxyClass: Class<*>? = null
    private var binaryMessageProxyClass: Class<*>? = null

    @RuntimeType
    fun delegatedTextMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as String)
        .also { logger.trace { "delegatedTextMessageData: Payload received: $it" } }
        .let(payloadProcessor::retrieveDrillHeaders)

    @RuntimeType
    fun delegatedBinaryMessageData(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = method.invoke(target).let { pooledProxyClass!!.constructors[0].newInstance(it)!! }

    @RuntimeType
    @Suppress("unchecked_cast")
    fun delegatedPooledResource(
        @Origin method: Method,
        @FieldValue("target") target: Any
    ) = (method.invoke(target) as Array<ByteBuffer>).let { buffers ->
        val isSimpleArray = buffers.size == 1 &&
                buffers[0].hasArray() &&
                buffers[0].arrayOffset() == 0 &&
                buffers[0].position() == 0 &&
                buffers[0].array().size == buffers[0].remaining()
        val array: ByteArray = if (isSimpleArray) {
            buffers[0].array()
        }
        else {
            Class.forName("org.xnio.Buffers", true, target::class.java.classLoader)
                .getMethod("take", Array<ByteBuffer>::class.java, Int::class.java, Int::class.java)
                .invoke(null, buffers, 0, buffers.size) as ByteArray
        }
        array.also { logger.trace { "delegatedPooledResource: Payload received: ${it.decodeToString()}" } }
            .let(payloadProcessor::retrieveDrillHeaders)
            .let(ByteBuffer::wrap)
            .let { arrayOf(it) }
    }

    fun getTextMessageProxy(classPool: ClassPool) = textMessageProxyClass
        ?: createTextMessageProxy(classPool).also(::textMessageProxyClass::set)

    fun getBinaryMessageProxy(classPool: ClassPool) = binaryMessageProxyClass
        ?: createBinaryMessageProxy(classPool).also(::binaryMessageProxyClass::set).also { getPooledProxy(classPool) }

    private fun getPooledProxy(classPool: ClassPool) = pooledProxyClass
        ?: createPooledProxy(classPool).also(::pooledProxyClass::set)

    private fun createTextMessageProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "io.undertow.websockets.core.BufferedTextMessage",
        "getData",
        ::delegatedTextMessageData,
        classPool
    ) { MethodCall.invoke(it.getConstructor(Boolean::class.java)).with(false) }

    private fun createBinaryMessageProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "io.undertow.websockets.core.BufferedBinaryMessage",
        "getData",
        ::delegatedBinaryMessageData,
        classPool
    ) { MethodCall.invoke(it.getConstructor(Boolean::class.java)).with(false) }

    private fun createPooledProxy(classPool: ClassPool) = createDelegatedGetterProxy(
        "org.xnio.Pooled",
        "getResource",
        ::delegatedPooledResource,
        classPool,
    ) { MethodCall.invoke(Any::class.java.getConstructor()) }

}
