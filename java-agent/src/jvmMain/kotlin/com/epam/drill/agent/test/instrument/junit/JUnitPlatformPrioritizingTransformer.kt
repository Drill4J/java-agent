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

import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.test.execution.TestMethodInfo
import com.epam.drill.agent.test.prioritization.RecommendedTests
import javassist.*
import mu.KotlinLogging
import java.security.ProtectionDomain

actual object JUnitPlatformPrioritizingTransformer : TransformerObject, AbstractJUnitTransformer() {

    override val logger = KotlinLogging.logger {}
    private val DrillJUnit5Filter = "${this.javaClass.`package`.name}.gen.DrillJUnit5Filter"
    private val LauncherDiscoveryRequestAdapter = "${this.javaClass.`package`.name}.gen.LauncherDiscoveryRequestAdapter"

    override fun enabled(): Boolean = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED]

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/platform/launcher/core/DefaultLauncher"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        createRecommendedTestsFilterClass(pool, classLoader, protectionDomain)
        createLauncherDiscoveryRequestAdapterClass(pool, classLoader, protectionDomain)
        instrumentDiscoverMethod(ctClass)
        instrumentExecuteMethod(ctClass)
    }

    private fun instrumentDiscoverMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "discover",
            "(Lorg/junit/platform/launcher/LauncherDiscoveryRequest;)Lorg/junit/platform/launcher/TestPlan;"
        ).insertBefore(
            """                
                $1 = new $LauncherDiscoveryRequestAdapter($1, new $DrillJUnit5Filter());
            """.trimIndent()
        )
    }

    private fun instrumentExecuteMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "execute",
            "(Lorg/junit/platform/launcher/LauncherDiscoveryRequest;[Lorg/junit/platform/launcher/TestExecutionListener;)V")
            .insertBefore(
            """
                $1 = new $LauncherDiscoveryRequestAdapter($1, new $DrillJUnit5Filter());
            """.trimIndent()
        )
    }

    private fun createRecommendedTestsFilterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc: CtClass = pool.makeClass(DrillJUnit5Filter)
        cc.interfaces = arrayOf(pool.get(PostDiscoveryFilter))
        cc.addMethod(
            CtMethod.make(
                """
                       public $FilterResult apply(java.lang.Object object) {                            
                            $TestDescriptor descriptor = ($TestDescriptor)object;
                            if (descriptor.isContainer())
                               return $FilterResult.included("");
                               
                            ${getMetadata("descriptor")}   
                            ${getTags("descriptor")}          
                                                                                                               
                            ${TestMethodInfo::class.java.name} methodInfo = ${this::class.java.name}.INSTANCE.${this::convertToMethodInfo.name}(testMetadata, testTags, descriptor.getDisplayName());
                            boolean shouldSkip = methodInfo != null && ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkipByTestMethod.name}(methodInfo);
                            if (shouldSkip) {                                
                                return $FilterResult.excluded("skipped by Drill4J");
                            } else {
                                return $FilterResult.included("recommended by Drill4J");
                            }                                                    		                    
	                   }
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }

    private fun createLauncherDiscoveryRequestAdapterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc: CtClass = pool.makeClass(LauncherDiscoveryRequestAdapter)
        cc.interfaces = arrayOf(pool.get(LauncherDiscoveryRequest))
        cc.addField(CtField.make("$LauncherDiscoveryRequest delegate = null;", cc))
        cc.addField(CtField.make("$PostDiscoveryFilter additionalFilter = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public LauncherDiscoveryRequestAdapter($LauncherDiscoveryRequest delegate, $PostDiscoveryFilter additionalFilter) {
                        this.delegate = delegate;
                        this.additionalFilter = additionalFilter;
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public java.util.List getEngineFilters() {
                        return delegate.getEngineFilters();
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getPostDiscoveryFilters() {
                    java.util.ArrayList modifiedList = new java.util.ArrayList(delegate.getPostDiscoveryFilters());
                    modifiedList.add(additionalFilter);
                    return modifiedList;                                          
                }
            """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getSelectorsByType(java.lang.Class selectorType) {
                    return delegate.getSelectorsByType(selectorType);
                }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getFiltersByType(java.lang.Class filterType) {
                    return delegate.getFiltersByType(filterType);
                }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public $ConfigurationParameters getConfigurationParameters() {
                    return delegate.getConfigurationParameters();
                }
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun convertToMethodInfo(testMetadata: Map<String, String>,
                            testTags: List<String>,
                            displayName: String?): TestMethodInfo? {
        val testPath = testMetadata["class"] ?: testMetadata["feature"] ?: testMetadata["suite"]
        val testName = testMetadata["method"]?.substringBefore("(") ?: displayName
        if (testPath == null || testName == null) {
            logger.error { "Failed to convert test metadata to TestDetails: $testMetadata" }
            return null
        }
        return TestMethodInfo(
            engine = testMetadata["engine"] ?: "junit",
            className = testPath,
            method = testName,
            tags = testTags,
            metadata = testMetadata
        )
    }
}