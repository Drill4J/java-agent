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
package com.epam.drill.agent.test.instrument.testng

import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.prioritization.RecommendedTests
import javassist.ClassPool
import javassist.CtClass
import java.security.ProtectionDomain

abstract class AbstractTestNGPrioritizingTransformer() : AbstractTestNGTransformer() {
    private val engineSegment = "testng"
    abstract val versionRegex: Regex
    abstract fun getMethodParametersExpression(): String

    override fun enabled(): Boolean = super.enabled() && agentConfiguration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return interfaces.any { it == "org/testng/IMethodSelector" }
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        if ("${ctClass.url}".contains(versionRegex)) {
            instrumentIfSupport(ctClass, pool, classLoader, protectionDomain)
        }
    }

    private fun instrumentIfSupport(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        instrumentIncludeMethod(ctClass)
        return ctClass.toBytecode()
    }

    private fun instrumentIncludeMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "includeMethod",
            "(Lorg/testng/IMethodSelectorContext;Lorg/testng/ITestNGMethod;Z)Z"
        ).insertAfter(
            """            
            if (${'$'}_ == true && $3 == true) {                
                java.lang.String className = $2.getTestClass().getName();
                java.lang.String methodName = $2.getMethodName();
                java.lang.String methodParameters = ${this::class.java.name}.INSTANCE.${this::paramTypes.name}($2.${getMethodParametersExpression()});                
                boolean shouldSkip = ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkip.name}("$engineSegment", className, methodName, methodParameters);
                if (shouldSkip) {
                    return false;
                }
            }                        
        """.trimIndent()
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun paramTypes(objects: Array<Class<*>?>?): String = objects?.joinToString(",", "(", ")") { obj ->
        obj?.name ?: ""
    } ?: ""

}