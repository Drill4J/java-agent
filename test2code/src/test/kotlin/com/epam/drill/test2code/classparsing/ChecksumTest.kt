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

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.Test
import com.epam.drill.fixture.ast.*
import com.epam.drill.fixture.ast.OverrideTest
// TODO
//  -make test cases independent (split fixture class into multiple classes)
//  -rewrite tests cases testing change ("Build1" and "Build2")
class ChecksumTest {

    private val checksumsBuild1 = calculateMethodsChecksums(Build1::class.readBytes(), Build1::class.getFullName())
    private val checksumsBuild2 = calculateMethodsChecksums(Build2::class.readBytes(), Build2::class.getFullName())

    @Test
    fun `lambda with different context should have other checksum`() {
        val methodName = "lambda\$differentContextChangeAtLambda\$1/java.lang.String/java.lang.String"
        // Lambdas should have different value, because they contain different context
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `constant lambda hash calculation`() {
        val methodName = "lambda\$constantLambdaHashCalculation\$6/java.lang.String/java.lang.String"
        assertEquals(
            checksumsBuild1[methodName],
            checksumsBuild2[methodName]
        )
    }

    @Test
    fun `method with lambda should have the same checksum`() {
        val methodName = "theSameMethodBody"
        //Methods, which contain lambda with different context, should not have difference in checksum
        assertEquals(
            checksumsBuild1[methodName],
            checksumsBuild2[methodName]
        )
    }

    @Test
    fun `test on different context at inner lambda`() {
        val methodName = "lambda\$null\$2/java.lang.String/java.lang.String"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `should have different checksum for reference call`() {
        val methodName = "referenceMethodCall/java.util.List/void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `should have different checksum for array`() {
        val methodName = "multiANewArrayInsnNode//void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `should have different checksum for switch table`() {
        val methodName = "tableSwitchMethodTest/int/void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `should have different checksum for look up switch`() {
        val methodName = "lookupSwitchMethodTest/java.lang.Integer/void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `method with different instance type`() {
        val methodName = "differentInstanceType//void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

    @Test
    fun `method call other method`() {
        val methodName = "callOtherMethod//void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }
//    Do we need to cover that case?
//    @Test
//    fun `method with different params`() {
//        val methodName = "methodWithDifferentParams"
//        assertChecksum(methodName)
//    }

    @Test
    fun `method with incremented value`() {
        val methodName = "methodWithIteratedValue/java.lang.Integer/void"
        assertChecksum(methodName, checksumsBuild1, checksumsBuild2)
    }

//   Do we need to cover that case?
//    @Test
//    fun `change name of local var`() {
//        val methodName = "changeLocalVarName"
//        assertChecksum(methodName)
//    }

}

class ConstructorTest {

    private val checksumsBuild1 =
        calculateMethodsChecksums(ConstructorTestBuild1::class.readBytes(), ConstructorTestBuild1::class.getFullName())
    private val checksumsBuild2 =
        calculateMethodsChecksums(ConstructorTestBuild2::class.readBytes(), ConstructorTestBuild2::class.getFullName())

    @Test
    fun `class with more that one constructor should have two different checksum`() {
        val constructorName =
            "<init>/java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String/void"
        val defaultConstructorName = "<init>//void"
        assertChecksum(constructorName, checksumsBuild1, checksumsBuild2)
        assertEquals(
            checksumsBuild1[defaultConstructorName],
            checksumsBuild2[defaultConstructorName]
        )
    }
}

class OverrideTest {

    private val methodsBuild =
        calculateMethodsChecksums(OverrideTest::class.readBytes(), OverrideTest::class.getFullName())

    @Test
    fun `class with override methods should have different checksum`() {
        val methodNameReal = "call//java.lang.String"
        val methodNameVirtual = "call//java.lang.Object"
        assertEquals(3, methodsBuild.size)
        //Compare checksum of virtual method and method with override annotation
        assertNotEquals(
            methodsBuild[methodNameReal],
            methodsBuild[methodNameVirtual]
        )
    }
}

private fun assertChecksum(
    methodName: String,
    build1: Map<String, String>,
    build2: Map<String, String>
) {
    assertNotEquals(
        build1[methodName],
        build2[methodName]
    )
}
