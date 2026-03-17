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
package com.epam.drill.agent.test.instrument.cucumber

import com.epam.drill.agent.instrument.InstrumentationParameterDefinitions.INSTRUMENTATION_CUCUMBER_ENABLED
import com.epam.drill.agent.test.execution.TestController
import com.epam.drill.agent.test.instrument.AbstractTestTransformerObject
import javassist.*
import mu.KotlinLogging
import java.security.*

abstract class AbstractCucumberTransformer() : AbstractTestTransformerObject() {
    val engineSegment = "cucumber"
    val EventBusProxy = "EventBusProxy"

    override val logger = KotlinLogging.logger {}
    abstract val Status: String
    abstract val EventBus: String
    abstract val EventHandler: String
    abstract val Event: String
    abstract val PickleStepDefinitionMatch: String
    abstract val testPackage: String

    override fun enabled() = super.enabled() && agentConfiguration.parameters[INSTRUMENTATION_CUCUMBER_ENABLED]

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        val cc: CtClass = pool.makeClass(EventBusProxy)
        cc.interfaces = arrayOf(pool.get(EventBus))
        cc.addField(CtField.make("$EventBus mainEventBus = null;", cc))
        cc.addField(CtField.make("String featurePath = \"\";", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public $EventBusProxy($EventBus mainEventBus, String featurePath) {
                        this.mainEventBus = mainEventBus;
                        this.featurePath = featurePath;
                    }
                """.trimMargin(),
                cc
            )
        )
        cc.implEventBusMethods()

        cc.addMethod(
            CtMethod.make(
                """
                    public void send($Event event) {
                        mainEventBus.send(event);   
                        if (event instanceof $testPackage.TestStepStarted) {
                            ${TestController::class.java.name}.INSTANCE.${TestController::testStarted.name}("$engineSegment", featurePath, (($testPackage.TestStepStarted) event).getTestCase().getName());  
                        } else if (event instanceof $testPackage.TestStepFinished) {
                            $testPackage.TestStepFinished finishedTest = ($testPackage.TestStepFinished) event;
                            $Status status = ${getTestStatus()}
                            if (status != $Status.PASSED) {
                                status = $Status.FAILED;
                            }
                            ${TestController::class.java.name}.INSTANCE.${TestController::testFinished.name}("$engineSegment", featurePath, finishedTest.getTestCase().getName(), status.name());                                    
                        }
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public void sendAll(Iterable queue) {
                        mainEventBus.sendAll(queue);
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                        public void removeHandlerFor(Class aClass, $EventHandler eventHandler) { 
                            mainEventBus.removeHandlerFor(aClass, eventHandler);
                        }
                    """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                   public void registerHandlerFor(Class aClass, $EventHandler eventHandler) {
                            mainEventBus.registerHandlerFor(aClass, eventHandler);
                   } 
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)

        /**
         *      {@link PickleStepDefinitionMatch} is represent a step of scenario.
         *      Check for PickleStepDefinitionMatch is needed to determine what we are currently performing,
         *      a step from a scenario or before or after action.
         *      Instead of the class name, we use the path to the feature file.
         *      If the file is in the same repository as the tests, then we take the relative path,
         *      otherwise we take the absolute path without specifying the disk name
         */
        ctClass.getDeclaredMethod("run").insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof $PickleStepDefinitionMatch) {
                        ${getFeaturePath()}
                        $2 = new $EventBusProxy($2, featurePath);
                    }
                } catch (Throwable ignored) {}
            """.trimIndent()
        )
    }

    abstract fun getFeaturePath(): String

    abstract fun getTestStatus(): String

    abstract fun CtClass.implEventBusMethods()
}
