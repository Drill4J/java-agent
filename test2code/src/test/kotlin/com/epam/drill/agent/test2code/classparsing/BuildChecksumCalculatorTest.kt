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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.epam.drill.agent.test2code.common.api.AstMethod

class BuildChecksumCalculatorTest {

    private fun expectedChecksum(vararg checksums: String): String =
        checksums.fold(0L) { acc, c -> acc + c.toLong(CHECKSUM_RADIX) }.toString(CHECKSUM_RADIX)

    private fun String.astMethod() = AstMethod(
        classname = "Test",
        name = "test",
        params = "",
        returnType = "void",
        probesCount = 0,
        probesStartPos = 0,
        bodyChecksum = this
    )

    @Test
    fun `should produce base-36 checksum equal to the sum of parsed values`() {
        val a = 123456789L.toString(CHECKSUM_RADIX)
        val b = (-987654321L).toString(CHECKSUM_RADIX)
        val calculator = CumulativeChecksumCalculator()

        calculator.add(a.astMethod())
        calculator.add(b.astMethod())

        assertEquals(expectedChecksum(a, b), calculator.methodsChecksum)
        assertEquals(2, calculator.methodsCount)
    }

    @Test
    fun `should be order independent`() {
        val checksums = listOf(1L, 42L, -7L, Long.MAX_VALUE, Long.MIN_VALUE)
            .map { it.toString(CHECKSUM_RADIX) }
            .map { it.astMethod() }

        val forward = CumulativeChecksumCalculator().apply { checksums.forEach(::add) }
        val backward = CumulativeChecksumCalculator().apply { checksums.reversed().forEach(::add) }

        assertEquals(forward.methodsChecksum, backward.methodsChecksum)
    }

    @Test
    fun `should skip blank checksums from the sum but count them`() {
        val a = 555L.toString(CHECKSUM_RADIX)
        val calculator = CumulativeChecksumCalculator()

        calculator.add(a.astMethod())
        calculator.add("".astMethod())
        calculator.add("   ".astMethod())

        assertEquals(expectedChecksum(a), calculator.methodsChecksum)
        assertEquals(3, calculator.methodsCount)
    }

    @Test
    fun `should wrap on overflow like data-ingest fold`() {
        val max = Long.MAX_VALUE.toString(CHECKSUM_RADIX)
        val one = 1L.toString(CHECKSUM_RADIX)
        val calculator = CumulativeChecksumCalculator()

        calculator.add(max.astMethod())
        calculator.add(one.astMethod())

        assertEquals(Long.MIN_VALUE.toString(CHECKSUM_RADIX), calculator.methodsChecksum)
    }

    @Test
    fun `should throw InvalidChecksumException on non-base-36 checksum`() {
        val calculator = CumulativeChecksumCalculator()
        assertFailsWith<InvalidChecksumException> { calculator.add("not-a-checksum!".astMethod()) }
    }

    @Test
    fun `empty calculator should have zero checksum and no methods`() {
        val calculator = CumulativeChecksumCalculator()
        assertEquals(0L.toString(CHECKSUM_RADIX), calculator.methodsChecksum)
        assertEquals(0, calculator.methodsCount)
    }

}
