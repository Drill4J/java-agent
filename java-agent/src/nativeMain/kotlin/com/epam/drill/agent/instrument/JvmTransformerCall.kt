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

import com.epam.drill.jvmapi.getObjectMethod
import com.epam.drill.jvmapi.toByteArray
import com.epam.drill.jvmapi.toJByteArray
import com.epam.drill.jvmapi.gen.CallObjectMethod
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.gen.jobject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
internal fun callTransformerTransformMethod(
    clazz: KClass<out Any>,
    method: KCallable<ByteArray?>,
    className: String,
    classFileBuffer: ByteArray,
    loader: Any?,
    protectionDomain: Any?
) = getObjectMethod(clazz, method.name, "(Ljava/lang/String;[BLjava/lang/Object;Ljava/lang/Object;)[B").run {
    CallObjectMethod(
        this.first,
        this.second,
        NewStringUTF(className),
        toJByteArray(classFileBuffer),
        loader as jobject?,
        protectionDomain as jobject?
    )?.let(::toByteArray)
}

@Suppress("UNCHECKED_CAST")
internal fun callTTLTransformerTransformMethod(
    clazz: KClass<out TTLTransformer>,
    method: KCallable<ByteArray?>,
    loader: Any?,
    classFile: String?,
    classBeingRedefined: Any?,
    classFileBuffer: ByteArray
) = getObjectMethod(clazz, method.name, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;[B)[B").run {
    CallObjectMethod(
        this.first,
        this.second,
        loader as jobject?,
        NewStringUTF(classFile),
        classBeingRedefined as jobject?,
        toJByteArray(classFileBuffer)
    )?.let(::toByteArray)
}
