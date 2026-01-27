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

import javassist.*

abstract class AbstractCucumber56Transformer() : AbstractCucumberTransformer() {
    override val Status = "io.cucumber.plugin.event.Status"
    override val EventBus: String = "io.cucumber.core.eventbus.EventBus"
    override val EventHandler = """io.cucumber.plugin.event.EventHandler"""
    override val PickleStepDefinitionMatch: String = "io.cucumber.core.runner.PickleStepDefinitionMatch"
    override val testPackage = "io.cucumber.plugin.event"
    abstract val versionPattern: Regex

    /**
     * From cucumber 5 TestStep class location doesn't change
     */
    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "io/cucumber/core/runner/TestStep"
    }

    override fun getFeaturePath(): String = """
         String[] paths = new java.io.File(".").toURI().relativize($1.getUri()).toString().split(":");
         int index = paths.length - 1;
         String featurePath = paths[index];
         if (featurePath.startsWith("/")) {
            featurePath = featurePath.replaceFirst("/", "");
         }
    """.trimIndent()

    override fun getTestStatus(): String = """finishedTest.getResult().getStatus();""".trimIndent()

    override fun CtClass.implEventBusMethods() {
        addMethod(
            CtMethod.make(
                """
                        public java.time.Instant getInstant() {
                            return mainEventBus.getInstant();
                        }
                    """.trimIndent(),
                this
            )
        )
        addMethod(
            CtMethod.make(
                """
                        public java.util.UUID generateId() {
                            return mainEventBus.generateId();
                        }
                    """.trimIndent(),
                this
            )
        )
    }
}
