/**
 * Copyright 2020 EPAM Systems
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
@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.threadpool.agent.internal.javassist.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import java.io.*
import kotlin.reflect.jvm.*

@Kni
actual object Transformer {
    private val logger = Logging.logger(Transformer::class.jvmName)
    private val classPool = ClassPool()

    actual fun transform(className: String, classfileBuffer: ByteArray, loader: Any?): ByteArray? {
        return try {
            classPool.appendClassPath(LoaderClassPath(loader as? ClassLoader))
            classPool.makeClass(ByteArrayInputStream(classfileBuffer))?.run {
                if (interfaces.isNotEmpty() && interfaces.map { it.name }
                        .contains("javax.servlet.ServletContextListener")) {
                    val qualifiedName = WebContainerSource::class.qualifiedName
                    val fillWeSourceMethodName = WebContainerSource::fillWebAppSource.name
                    declaredMethods.firstOrNull { it.name == "contextInitialized" }?.insertBefore(
                        "try{$qualifiedName.INSTANCE.$fillWeSourceMethodName(\$1.getServletContext().getRealPath(\"/\"),\$1.getServletContext().getResource(\"/\"));}catch(java.lang.Throwable e){}"
                    ) ?: run {
                        logger.info { "Can't find 'contextInitialized' for class ${this.name}. Allowed methods ${declaredMethods.map { it.name }} " }
                        return null
                    }
                    return toBytecode()
                } else
                    null

            }
        } catch (e: Exception) {
            logger.warn(e) { "Instrumentation error" }
            null
        }
    }
}
