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
import com.epam.drill.agent.test.execution.TestMethodInfo
import javassist.*
import mu.KotlinLogging
import java.security.*

actual object JUnit5Transformer: TransformerObject, AbstractJUnitTransformer() {
    override val logger = KotlinLogging.logger {}

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/platform/engine/support/hierarchical/NodeTestTaskContext"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        val cc: CtClass = pool.makeClass("MyList")
        cc.interfaces = arrayOf(pool.get("org.junit.platform.engine.EngineExecutionListener"))
        cc.addField(CtField.make("org.junit.platform.engine.EngineExecutionListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public MyList(org.junit.platform.engine.EngineExecutionListener mainRunner) { 
                        this.mainRunner = mainRunner;
                    }
                        """.trimMargin(), cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void dynamicTestRegistered(org.junit.platform.engine.TestDescriptor testDescriptor) {
                        mainRunner.dynamicTestRegistered(testDescriptor);
                    }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void executionSkipped(org.junit.platform.engine.TestDescriptor testDescriptor, String reason) {
                        mainRunner.executionSkipped(testDescriptor, reason);                        
                        if (!testDescriptor.isContainer()) {
                            ${getMetadata("testDescriptor")}
                            ${getTags("testDescriptor")}
                            ${TestMethodInfo::class.java.name} methodInfo = ${this::class.java.name}.INSTANCE.${this::convertToMethodInfo.name}(testMetadata, testTags);
                            if (methodInfo != null) {
                                ${TestController::class.java.name}.INSTANCE.${TestController::recordTestIgnoring.name}(methodInfo, false);
                            }                            
                        }
                    }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void executionStarted(org.junit.platform.engine.TestDescriptor testDescriptor) {
                        mainRunner.executionStarted(testDescriptor);
                        if (!testDescriptor.isContainer()) {
                            ${getMetadata("testDescriptor")}
                            ${getTags("testDescriptor")}
                            ${TestMethodInfo::class.java.name} methodInfo = ${this::class.java.name}.INSTANCE.${this::convertToMethodInfo.name}(testMetadata, testTags);
                            if (methodInfo != null) {
                                ${TestController::class.java.name}.INSTANCE.${TestController::recordTestStarting.name}(methodInfo);
                            } 
                        }
                    }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void executionFinished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.TestExecutionResult testExecutionResult) {
                        mainRunner.executionFinished(testDescriptor, testExecutionResult);
                        if (!testDescriptor.isContainer()) {
                            ${getMetadata("testDescriptor")}
                            ${getTags("testDescriptor")}
                            ${TestMethodInfo::class.java.name} methodInfo = ${this::class.java.name}.INSTANCE.${this::convertToMethodInfo.name}(testMetadata, testTags);
                            if (methodInfo != null) {
                                ${TestController::class.java.name}.INSTANCE.${TestController::recordTestFinishing.name}(methodInfo, testExecutionResult.getStatus().name());
                            }
                        }
                    }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void reportingEntryPublished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.reporting.ReportEntry entry) {
                        mainRunner.reportingEntryPublished(testDescriptor, entry);
                    }
                        """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)

        ctClass.constructors.first().insertBefore(
            """
                    $1 = new MyList($1);
            """.trimIndent()
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun convertToMethodInfo(
        testMetadata: Map<String, String>,
        testTags: List<String>
    ): TestMethodInfo? {
        return TestMethodInfo(
            engine = testMetadata["engine"] ?: "junit",
            className = testMetadata["class"] ?: return null,
            method = testMetadata["method"]?.substringBefore("(") ?: return null,
            methodParams = testMetadata["method"]?.getMethodParams() ?: "()",
            metadata = testMetadata,
            tags = testTags
        )
    }

    private fun String.getMethodParams(): String {
        val params = this.substringAfter("(").substringBefore(")")
        return if (params.isEmpty()) "()" else "($params)"
    }
}
