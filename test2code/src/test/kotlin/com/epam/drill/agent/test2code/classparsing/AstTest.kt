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
package com.epam.drill.agent.test2code.classparsing

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.Test
import com.epam.drill.agent.fixture.ast.CheckProbeRanges
import com.epam.drill.agent.fixture.ast.SimpleClass
import com.epam.drill.agent.test2code.common.api.AstMethod
import kotlin.test.assertTrue

class AstTest {

    @Test
    fun `check parsing method signature`() {
        val astMethods = parseAstClass(SimpleClass::class.getFullName(), SimpleClass::class.readBytes())
        assertTrue(astMethods.all { it.classname == SimpleClass::class.getFullName() })
        assertEquals(4, astMethods.size)
        astMethods[0].run {
            assertEquals("<init>", name)
            assertNotNull(bodyChecksum)
        }
        astMethods[1].run {
            assertEquals("simpleMethod", name)
            assertNotNull(bodyChecksum)
        }
        astMethods[2].run {
            assertEquals("methodWithReturn", name)
            assertEquals("java.lang.String", returnType)
            assertNotNull(bodyChecksum)
        }
        astMethods[3].run {
            assertEquals("methodWithParams", name)
            assertEquals("java.lang.String,int", params)
            assertNotNull(bodyChecksum)
        }
    }


    //FYI: ProbeRange test-cases
    private val methods = parseAstClass(CheckProbeRanges::class.getFullName(), CheckProbeRanges::class.readBytes())
        .groupBy { it.name }.mapValues { it.value[0] }

    @Test
    fun `test setting probes for empty method`() {
        /**
         *     void noOp() {
         *         AgentProbes var1 = ...
         *         var1.set(1);
         *     }
         */
        methods["noOp"]
            .assertProbesCount(1)
            .assertProbeStartPosition(1)
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
        methods["oneOp"]
            .assertProbesCount(1)
            .assertProbeStartPosition(2)
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
        methods["twoOps"]
            .assertProbesCount(2)
            .assertProbeStartPosition(3)
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
        methods["ifOp"]
            .assertProbesCount(2)
            .assertProbeStartPosition(5)
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
        methods["ifExpr"]
            .assertProbesCount(3)
            .assertProbeStartPosition(7)
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
        methods["whileOp"]
            .assertProbesCount(3)
            .assertProbeStartPosition(10)
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
        methods["methodWithLambda"]
            .assertProbesCount(1)
            .assertProbeStartPosition(13)
        methods["lambda\$methodWithLambda\$0"]
            .assertProbesCount(1)
            .assertProbeStartPosition(15)
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
        methods["methodRef"]
            .assertProbesCount(1)
            .assertProbeStartPosition(14)
    }
}

internal fun KClass<*>.readBytes(): ByteArray = java.getResourceAsStream(
    "/${getFullName()}.class"
)!!.readBytes()

internal fun KClass<*>.getFullName() = java.name.replace('.', '/')

internal fun AstMethod?.assertProbesCount(count: Int): AstMethod {
    assertNotNull(this)
    assertEquals(count, this.probesCount)
    return this
}

internal fun AstMethod?.assertProbeStartPosition(position: Int): AstMethod {
    assertNotNull(this)
    assertEquals(position, this.probesStartPos)
    return this
}
