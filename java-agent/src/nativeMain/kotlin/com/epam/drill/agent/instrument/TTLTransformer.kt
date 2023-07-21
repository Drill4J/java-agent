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

actual object TTLTransformer {
    val directTtlClasses = listOf(
        "java/util/concurrent/ScheduledThreadPoolExecutor",
        "java/util/concurrent/ThreadPoolExecutor",
        "java/util/concurrent/ForkJoinTask",
        "java/util/concurrent/ForkJoinPool"
    )
    const val timerTaskClass = "java/util/TimerTask"
    const val runnableInterface = "java/lang/Runnable"
    const val poolExecutor = "java/util/concurrent/ThreadPoolExecutor"
    const val jdkInternal = "jdk/internal"

    actual fun transform(
        loader: Any?,
        classFile: String?,
        classBeingRedefined: Any?,
        classFileBuffer: ByteArray,
    ): ByteArray? =
        callTTLTransformerTransformMethod(
            TTLTransformer::class,
            TTLTransformer::transform,
            loader,
            classFile,
            classBeingRedefined,
            classFileBuffer
        )
}
