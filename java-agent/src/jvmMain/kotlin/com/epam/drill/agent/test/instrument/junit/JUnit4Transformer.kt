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

import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.test.execution.TestController
import com.epam.drill.agent.test.execution.TestResult
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.CtNewConstructor
import mu.KotlinLogging
import java.security.ProtectionDomain

actual object JUnit4Transformer:
    TransformerObject,
    AbstractJUnitTransformer() {
    override val logger = KotlinLogging.logger {}
    const val engineSegment = "junit"

    override fun permit(
        className: String,
        superName: String?,
        interfaces: Array<String?>
    ): Boolean {
        return className == "org/junit/runner/notification/RunNotifier"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ) {
        val cc: CtClass = pool.makeClass("MyList")
        cc.superclass = pool.get("org.junit.runner.notification.RunListener")
        cc.addField(CtField.make("org.junit.runner.notification.RunListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public MyList(org.junit.runner.notification.RunListener mainRunner) {
                        this.mainRunner = mainRunner;
                    }
                        """.trimMargin(), cc
            )
        )
        val dp = """description"""
        cc.addMethod(
            CtMethod.make(
                """
                    public void testRunStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testRunStarted($dp);
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void testStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testStarted($dp);
                        ${TestController::class.java.name}.INSTANCE.${TestController::testStarted.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName());
                    }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                    public void testFinished(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testFinished(description);
                        ${TestController::class.java.name}.INSTANCE.${TestController::testFinished.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName(), "${TestResult.PASSED.name}");
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void testRunFinished(org.junit.runner.Result result) throws Exception {
                        this.mainRunner.testRunFinished(result);
                    }
                        """.trimIndent(),
                cc
            )
        )


        val failureParamName = """failure"""
        val desct = """$failureParamName.getDescription()"""
        cc.addMethod(
            CtMethod.make(
                """
                    public void testFailure(org.junit.runner.notification.Failure $failureParamName) throws Exception {
                        this.mainRunner.testFailure($failureParamName);
                        ${TestController::class.java.name}.INSTANCE.${TestController::testFinished.name}("$engineSegment", $desct.getClassName(), $desct.getMethodName(), "${TestResult.FAILED.name}");
                    }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                    public void testAssumptionFailure(org.junit.runner.notification.Failure $failureParamName) {
                        this.mainRunner.testAssumptionFailure(failure);
                    }
                        """.trimIndent(),
                cc
            )
        )



        cc.addMethod(
            CtMethod.make(
                """
                    public void testIgnored(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testIgnored($dp);
                        ${TestController::class.java.name}.INSTANCE.${TestController::testIgnored.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName());      
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.toClass(classLoader, protectionDomain)
        ctClass.getDeclaredMethod("addListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("addFirstListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
    }
}