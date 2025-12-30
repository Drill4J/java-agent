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
import java.security.ProtectionDomain
import javassist.ClassPool
import javassist.CtClass

actual object Cucumber5Transformer : TransformerObject, AbstractCucumber56Transformer() {
    override val versionPattern: Regex = "5\\.[0-9]+\\.[0-9]+".toRegex()
    override val Event: String = "io.cucumber.plugin.event.Event"

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        if ("${ctClass.url}".contains(versionPattern)) {
            super.instrument(ctClass, pool, classLoader, protectionDomain)
        }
    }
}
