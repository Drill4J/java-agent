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
package com.epam.drill.test2code.classparsing

import com.epam.drill.plugins.test2code.common.api.AstMethod
import com.epam.drill.fixture.ast.CheckProbeRanges
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProbeRangesTest {

    private val astEntity = parseAstClass(CheckProbeRanges::class.getFullName(), CheckProbeRanges::class.readBytes())
    private val methods = astEntity.methods.groupBy { it.name }.mapValues { it.value[0] }

    @Test
    fun `test setting probes for empty method`() {
        /**
         *     void noOp() {
         *         AgentProbes var1 = ...
         *         var1.set(1);
         *     }
         */
        methods["noOp"].assertProbesCount(1)
    }

    @Test
    fun `test setting probes for method with one operation`() {
        /**
         *     String oneOp() {
         *         AgentProbes var1 = ...
         *         var1.set(2);
         *         return "oneOp";
         *     }
         */
        methods["oneOp"].assertProbesCount(1)
    }

    @Test
    fun `test setting probes for method with two operations`() {
        /**
         *     void twoOps(List<String> list) {
         *         AgentProbes var2 = ...
         *         list.add("1");
         *         var2.set(3);
         *         list.add("2");
         *         var2.set(4);
         *     }
         */
        methods["twoOps"].assertProbesCount(2)
    }

    @Test
    fun `test setting probes for method with if operation`() {
        /**
         *     String ifOp(boolean b) {
         *         AgentProbes var2 = ...
         *         if (b) {
         *             var2.set(5);
         *             return "true";
         *         } else {
         *             var2.set(6);
         *             return "false";
         *         }
         *     }
         */
        methods["ifOp"].assertProbesCount(2)
    }

    @Test
    fun `test setting probes for method with if expression`() {
        /**
         *     String ifExpr(boolean b) {
         *         AgentProbes var2 = ...
         *         String var10000;
         *         if (b) {
         *             var10000 = "true";
         *             var2.set(7);
         *         } else {
         *             var10000 = "false";
         *             var2.set(8);
         *         }
         *
         *         var2.set(9);
         *         return var10000;
         *     }
         */
        methods["ifExpr"].assertProbesCount(3)
    }

    @Test
    fun `test setting probes for method with while loop`() {
        /**
         *     void whileOp(List<String> list) {
         *         AgentProbes var2 = ...
         *
         *         while(!list.isEmpty()) {
         *             var2.set(10);
         *             list.remove(0);
         *             var2.set(11);
         *         }
         *
         *         var2.set(12);
         *     }
         */
        methods["whileOp"].assertProbesCount(3)
    }

    @Test
    fun `test setting probes for method with lambda expression`() {
        /**
         *     void methodWithLambda(List<String> list) {
         *         AgentProbes var2 = ...
         *         list.forEach((s) -> {
         *            AgentProbes var1 = ...
         *            System.out.println(s);
         *            var1.set(15);
         *         });
         *         var2.set(13);
         *     }
         */
        methods["methodWithLambda"].assertProbesCount(1)
        methods["lambda\$methodWithLambda\$0"].assertProbesCount(1)
    }

    @Test
    fun `test setting probes for method with method reference`() {
        /**
         *     void methodRef(List<String> list) {
         *         AgentProbes var2 = ...
         *         PrintStream var10001 = System.out;
         *         list.forEach(var10001::println);
         *         var2.set(14);
         *     }
         */
        methods["methodRef"].assertProbesCount(1)
    }

}

internal fun AstMethod?.assertProbesCount(count: Int) {
    assertNotNull(this)
    assertEquals(count, this.probes.size)
}
