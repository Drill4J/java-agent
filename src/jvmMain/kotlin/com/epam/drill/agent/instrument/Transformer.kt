@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill.agent.instrument

import com.alibaba.ttl.internal.javassist.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.logging.*
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
                    declaredMethods.firstOrNull { it.name == "contextInitialized" }?.insertBefore(
                        "try{$qualifiedName.INSTANCE.$fillWeSourceMethodName(\$1.getServletContext().getRealPath(\"/\"),\$1.getServletContext().getResource(\"/\"));}catch(java.lang.Throwable e){}"
                    ) ?: run {
                        log(Level.INFO) { "!!!Can't find 'contextInitialized' for class ${this.name}. Allowed methods ${declaredMethods.map { it.name }} " }
                        return null
                    }
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