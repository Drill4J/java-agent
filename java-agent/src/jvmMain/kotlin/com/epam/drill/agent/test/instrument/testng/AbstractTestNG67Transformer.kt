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

import com.epam.drill.agent.test.execution.TestController
import javassist.*
import java.lang.reflect.*
import java.security.*
import mu.KotlinLogging

abstract class AbstractTestNG67Transformer() : AbstractTestNGTransformer() {
    companion object {
        const val engineSegment = "testng"
        const val TestNGMethod = "org.testng.internal.TestNGMethod"
        const val ITestResult = "org.testng.ITestResult"

        private const val DrillTestNGTestListner = "DrillTestNGTestListener"
        private const val ITestContext = "org.testng.ITestContext"
    }

    override val logger = KotlinLogging.logger {}

    abstract val versionRegex: Regex

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/testng/TestRunner"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        createTestListener(pool, classLoader, protectionDomain)
        ctClass.constructors.forEach { it.insertAfter("addTestListener(new $DrillTestNGTestListner());") }
        ctClass.supportIgnoredTestsTracking()
    }

    private fun createTestListener(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        val testListener = pool.makeClass(DrillTestNGTestListner)
        testListener.interfaces = arrayOf(pool.get("org.testng.ITestListener"))
        testListener.addMethod(
            CtMethod.make(getParamsMethod(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(getTestClassNameMethod(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(getTestNameMethod(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(getTestGroups(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestStart($ITestResult result) {
                            if (result.getThrowable() == null) {
                                ${TestController::class.java.name}.INSTANCE.${TestController::testStarted.name}("$engineSegment", getTestClassName(result), getTestName(result), getParamsString(result), getTestGroups(result));
                            } else {
                                ${this::class.java.name}.INSTANCE.${this::debug.name}("The start of the test " + result.getName() + " is ignored by the drill");
                            }
                        }
                """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                       public void onTestSuccess($ITestResult result) {
                            ${TestController::class.java.name}.INSTANCE.${TestController::testFinished.name}("$engineSegment", getTestClassName(result), getTestName(result), "PASSED", getParamsString(result), getTestGroups(result));
                       }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailure($ITestResult result) {
                            ${TestController::class.java.name}.INSTANCE.${TestController::testFinished.name}("$engineSegment", getTestClassName(result), getTestName(result), "FAILED", getParamsString(result), getTestGroups(result));      
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestSkipped($ITestResult result) {
                            ${TestController::class.java.name}.INSTANCE.${TestController::testIgnored.name}("$engineSegment", getTestClassName(result), getTestName(result), getParamsString(result), getTestGroups(result));     
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailedButWithinSuccessPercentage($ITestResult result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onStart($ITestContext result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onFinish($ITestContext result) {
                            return;            
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.toClass(classLoader, protectionDomain)
    }

    protected open fun getParamsMethod(): String = """
        private String getParamsString($ITestResult result) {
            Object[] parameters = result.getParameters();
            String paramString = ${this::class.java.name}.INSTANCE.${this::paramTypes.name}(parameters);            
            return paramString;
        }
    """.trimIndent()

    protected open fun getTestClassNameMethod(): String = """
        private String getTestClassName($ITestResult result) {            
            return result.getInstanceName();
        }
    """.trimIndent()

    protected open fun getTestNameMethod(): String = """
        private String getTestName($ITestResult result) {            
            return result.getName();
        }
    """.trimIndent()

    protected open fun getTestGroups(): String = """
        private java.util.List getTestGroups($ITestResult result) {            
            String[] groups = result.getMethod().getGroups();
            return java.util.Arrays.asList(groups);
        }
    """.trimIndent()


    private fun CtClass.supportIgnoredTestsTracking() = getDeclaredMethod("run").insertBefore(
        """
            java.util.Iterator disabledTests = getExcludedMethods().iterator();
            while(disabledTests.hasNext()) {                
                java.lang.Object baseMethod = disabledTests.next();                
                if (baseMethod instanceof $TestNGMethod) {
                    $TestNGMethod test = ($TestNGMethod) baseMethod;
                    ${TestController::class.java.name}.INSTANCE.${TestController::testIgnored.name}("$engineSegment", test.getTestClass().getName(), test.getMethodName());     
                }
            }
        """.trimIndent()
    )

    fun paramTypes(objects: Array<Any?>?): String = objects?.joinToString(",", "(", ")") { obj ->
        when (obj) {
            null -> obj.toString()
            is Field -> obj.type.simpleName
            else -> obj.javaClass.simpleName.substringBeforeLast("\$")
        }
    } ?: ""

    fun debug(message: String) {
        logger.debug { message }
    }

}
