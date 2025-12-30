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

import com.epam.drill.agent.instrument.TransformerObject
import javassist.*
import java.security.*

actual object TestNG6Transformer : TransformerObject, AbstractTestNG67Transformer() {
    override val versionRegex: Regex = "testng-6\\.[0-9]+(\\.[0-9]+)*".toRegex()

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        if ("${ctClass.url}".contains(versionRegex)) {
            super.instrument(ctClass, pool, classLoader, protectionDomain)
        }
    }


    override fun getTestClassNameMethod(): String = """
        private String getTestClassName($ITestResult result) {            
            return result.getTestClass().getName();
        }
    """.trimIndent()
}
