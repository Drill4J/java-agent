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

import com.epam.drill.agent.instrument.TransformerObject
import javassist.*

actual object Cucumber4Transformer : TransformerObject, AbstractCucumberTransformer() {
    override val testPackage = "cucumber.api.event"
    override val Status = "cucumber.api.Result.Type"
    override val EventBus = "cucumber.runner.EventBus"
    override val EventHandler: String = "cucumber.api.event.EventHandler"
    override val Event = "cucumber.api.event.Event"
    override val PickleStepDefinitionMatch = "cucumber.runner.PickleStepDefinitionMatch"

    override fun permit(className: String, superName: String?, interfaces: Array<String?>): Boolean {
        return className == /*4.x.x*/"cucumber/runner/TestStep"
    }

    override fun getFeaturePath(): String = """
         String[] paths = new java.io.File(".").toURI().resolve($1.getUri()).toString().split(":");
         int index = paths.length - 1;
         String featurePath = paths[index];
         if (featurePath.startsWith("/")) {
            featurePath = featurePath.replaceFirst("/", "");
         }
    """.trimIndent()

    override fun getTestStatus(): String = """finishedTest.result.getStatus();""".trimIndent()

    override fun CtClass.implEventBusMethods() {
        addMethod(
            CtMethod.make(
                """
                    public Long getTime() {
                        return mainEventBus.getTime();
                    }
                """.trimIndent(),
                this
            )
        )
        addMethod(
            CtMethod.make(
                """
                    public Long getTimeMillis() {
                        return mainEventBus.getTimeMillis();
                    }
                """.trimIndent(),
                this
            )
        )
    }
}
