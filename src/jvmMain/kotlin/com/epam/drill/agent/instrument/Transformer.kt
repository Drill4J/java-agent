@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.agent.classloading.*
import java.io.*


actual object Transformer {
    private val classPool = ClassPool()

    fun transform(className: String, classfileBuffer: ByteArray, loader: ClassLoader): ByteArray? {
        return try {
            classPool.appendClassPath(LoaderClassPath(loader))
            classPool.makeClass(ByteArrayInputStream(classfileBuffer))?.run {
                if (interfaces.isNotEmpty() && interfaces.map { it.name }
                        .contains("javax.servlet.ServletContextListener")) {
                    val qualifiedName = WebContainerSource::class.qualifiedName
                    val fillWeSourceMethodName = WebContainerSource::fillWebAppSource.name
                    declaredMethods.first { it.name == "contextInitialized" }.insertBefore(
                        "try{$qualifiedName.INSTANCE.$fillWeSourceMethodName(\$1.getServletContext().getResource(\"/\").getPath());}catch(java.lang.Throwable e){}"
                    )
                    return toBytecode()
                } else
                    null

            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}