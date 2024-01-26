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
package com.epam.drill.plugins.test2code

import com.epam.drill.fixture.ast.SimpleClass
import com.epam.drill.test2code.classparsing.parseAstClass
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AstTest {

    @Test
    fun `check parsing method signature`() {
        val astEntity = parseAstClass(SimpleClass::class.getFullName(), SimpleClass::class.readBytes())
        assertEquals(SimpleClass::class.simpleName, astEntity.name)
        assertEquals("com/epam/drill/fixture/ast", astEntity.path)
        assertEquals(4, astEntity.methods.size)
        astEntity.methods[0].run {
            assertEquals("<init>", name)
            assertNotNull(checksum)
        }
        astEntity.methods[1].run {
            assertEquals("simpleMethod", name)
            assertNotNull(checksum)
        }
        astEntity.methods[2].run {
            assertEquals("methodWithReturn", name)
            assertEquals("java.lang.String", returnType)
            assertNotNull(checksum)
        }
        astEntity.methods[3].run {
            assertEquals("methodWithParams", name)
            assertEquals(listOf("java.lang.String", "int"), params)
            assertNotNull(checksum)
        }
    }
}

internal fun KClass<*>.readBytes(): ByteArray = java.getResourceAsStream(
    "/${getFullName()}.class"
)!!.readBytes()

internal fun KClass<*>.getFullName() = java.name.replace('.', '/')
