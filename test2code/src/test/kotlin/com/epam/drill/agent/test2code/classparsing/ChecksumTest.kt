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

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.Test
import com.epam.drill.agent.fixture.ast.*
import com.epam.drill.agent.fixture.ast.OverrideTest

// TODO these are bad tests: lots of repetition, poorly prepared test data (Build1.java, Build2.java)
//  rewrite to actually test cases handled in codeToString
class ChecksumTest {

    private val checksumsBuild1 = calculateMethodsChecksums(Build1::class.readBytes(), Build1::class.getFullName())
    private val checksumsBuild2 = calculateMethodsChecksums(Build2::class.readBytes(), Build2::class.getFullName())

    @Test
    fun `lambda with no changes to body should have matching checksums`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:lambda\$constantLambdaHashCalculation\$6:java.lang.String:java.lang.String"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:lambda\$constantLambdaHashCalculation\$6:java.lang.String:java.lang.String"
        assertEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    @Test
    fun `difference in nested lambdas should not change method body checksum`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:theSameMethodBody:java.util.List:void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:theSameMethodBody:java.util.List:void"
        assertEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    @Test
    fun `changing lambda body should change it's checksum`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:lambda\$null\$2:java.lang.String:java.lang.String"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:lambda\$null\$2:java.lang.String:java.lang.String"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    // TODO this test actually performs check on a different _object_ and not just the method called by reference
    @Test
    fun `should have different checksum for reference call`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:referenceMethodCall:java.util.List:void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:referenceMethodCall:java.util.List:void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    // TODO this test changes so much in the method, I'm not sure what exactly are we testing here
    //   either rewrite or remove it
    @Test
    fun `should have different checksum for array`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:multiANewArrayInsnNode::void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:multiANewArrayInsnNode::void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    // TODO this test and the next one test arguably the same behavior
    @Test
    fun `changing rule in look up switch should change checksum`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:tableSwitchMethodTest:int:void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:tableSwitchMethodTest:int:void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    @Test
    fun `changing rule in look up switch should change checksum 2`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:lookupSwitchMethodTest:java.lang.Integer:void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:lookupSwitchMethodTest:java.lang.Integer:void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    @Test
    fun `changing local variable type should change checksum`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:differentInstanceType::void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:differentInstanceType::void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    // TODO no idea why these checksums should be different
    @Test
    fun `method call other method`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:callOtherMethod::void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:callOtherMethod::void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

    // TODO not sure it _should_ work like that, but that's what our codeToString implementation does
    @Test
    fun `changing operator ++ to += should change checksum`() {
        val methodSignatureBuild1 = "com/epam/drill/agent/fixture/ast/Build1:methodWithIteratedValue:java.lang.Integer:void"
        val methodSignatureBuild2 = "com/epam/drill/agent/fixture/ast/Build2:methodWithIteratedValue:java.lang.Integer:void"
        assertNotEquals(
            checksumsBuild1[methodSignatureBuild1],
            checksumsBuild2[methodSignatureBuild2]
        )
    }

}


class OverrideTest {

    private val methodsBuild =
        calculateMethodsChecksums(OverrideTest::class.readBytes(), OverrideTest::class.getFullName())

    // TODO I really doubt that - OverrideTest changes method _body_
    //  it has very little to do with methods being virtual/overwritten
    @Test
    fun `virtual and overwritten method should have different body checksums`() {
        val methodNameReal = "com/epam/drill/agent/fixture/ast/OverrideTest:call::java.lang.String"
        val methodNameVirtual = "com/epam/drill/agent/fixture/ast/OverrideTest:call::java.lang.Object"
        assertEquals(3, methodsBuild.size)
        //Compare checksum of virtual method and method with override annotation
        assertNotEquals(
            methodsBuild[methodNameReal],
            methodsBuild[methodNameVirtual]
        )
    }
}

