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
package com.epam.drill.agent.test.instrument.junit

import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.test.prioritization.RecommendedTests
import javassist.*
import mu.KotlinLogging
import java.security.ProtectionDomain

private const val Filter = "org.junit.runner.manipulation.Filter"
private const val Description = "org.junit.runner.Description"

actual object JUnit4PrioritizingTransformer : TransformerObject, AbstractJUnitTransformer() {
    override val logger = KotlinLogging.logger {}
    private val engineSegment = "junit"
    private val DrillJUnit4Filter = "${this.javaClass.`package`.name}.gen.DrillJUnit4Filter"

    override fun enabled(): Boolean = super<AbstractJUnitTransformer>.enabled() && agentConfiguration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/runners/JUnit4"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        createRecommendedTestsFilterClass(pool, classLoader, protectionDomain)
        instrumentConstructor(ctClass)
    }

    private fun instrumentConstructor(ctClass: CtClass) {
        ctClass.constructors.forEach { constructor ->
            constructor.insertAfter(
                """
                $DrillJUnit4Filter drillFilter = new $DrillJUnit4Filter();
                filter(drillFilter);
            """.trimIndent()
            )
        }
    }

    private fun createRecommendedTestsFilterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc = pool.makeClass(DrillJUnit4Filter, pool.get(Filter))
        cc.addMethod(
            CtMethod.make(
                """
                   public boolean shouldRun($Description description) {
                        if (!description.isTest()) return true;
                        java.lang.String className = description.getClassName();
                        if (className == null) return true;
                        java.lang.String methodName = description.getMethodName();
                        if (methodName == null) return true;
                        boolean shouldSkip = ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkip.name}("$engineSegment", className, methodName, null);                            
                        return !shouldSkip;                                                    		                    
                   }
            """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                   public java.lang.String describe() {
                        return "skip tests by Drill4J";                                                    		                    
                   }
            """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }
}