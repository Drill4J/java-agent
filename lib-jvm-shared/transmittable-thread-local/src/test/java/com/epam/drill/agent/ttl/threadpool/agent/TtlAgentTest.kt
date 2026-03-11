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
package com.epam.drill.agent.ttl.threadpool.agent

import com.epam.drill.agent.noTtlAgentRun
import com.epam.drill.agent.ttl.threadpool.agent.TtlAgent.splitCommaColonStringToKV
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.test.config.TestCaseConfig
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe

class TtlAgentTest : AnnotationSpec() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun defaultTestCaseConfig(): TestCaseConfig =
        TestCaseConfig(enabled = noTtlAgentRun())

    @Test
    fun test_splitCommaColonStringToKV() {
        splitCommaColonStringToKV(null).shouldBeEmpty()
        splitCommaColonStringToKV("").shouldBeEmpty()
        splitCommaColonStringToKV("   ").shouldBeEmpty()

        splitCommaColonStringToKV("k1,k2") shouldBe mapOf("k1" to "", "k2" to "")

        splitCommaColonStringToKV("   k1,   k2 ") shouldBe mapOf("k1" to "", "k2" to "")

        splitCommaColonStringToKV("ttl.agent.logger:STDOUT") shouldBe mapOf("ttl.agent.logger" to "STDOUT")
        splitCommaColonStringToKV("k1:v1,ttl.agent.logger:STDOUT") shouldBe
                mapOf("k1" to "v1", "ttl.agent.logger" to "STDOUT")


        splitCommaColonStringToKV("     k1     :v1  , ttl.agent.logger    :STDOUT   ") shouldBe
                mapOf("k1" to "v1", "ttl.agent.logger" to "STDOUT")

        splitCommaColonStringToKV("     k1     :v1  , ttl.agent.logger    :STDOUT   ,k3") shouldBe
                mapOf("k1" to "v1", "ttl.agent.logger" to "STDOUT", "k3" to "")
    }
}
