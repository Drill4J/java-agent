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
package com.epam.drill.agent.common.classloading

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassSourceTest {

    @Test
    fun `prefix matching`() {
        val prefixes = listOf("foo/bar", "!foo/bar/Bar", "!subclassOf:foo/bar/Foo")
        assertTrue { "foo/bar/Baz.class".let(::ClassSource).prefixMatches(prefixes) }
        assertTrue { "Lfoo/bar/Baz.class".let(::ClassSource).prefixMatches(prefixes, 1) }
        assertFalse { "foo/baz/Baz.class".let(::ClassSource).prefixMatches(prefixes) }
        assertFalse { "Lfoo/baz/Baz.class".let(::ClassSource).prefixMatches(prefixes, 1) }
        assertFalse { "foo/bar/Bar.class".let(::ClassSource).prefixMatches(prefixes) }
        assertFalse { "Lfoo/bar/Bar.class".let(::ClassSource).prefixMatches(prefixes, 1) }
        assertFalse {
            ClassSource(
                entityName = "foo/bar/Baz.class",
                superName = "foo/bar/Foo.class"
            ).prefixMatches(prefixes)
        }
        assertFalse {
            ClassSource(
                entityName = "Lfoo/bar/Baz.class",
                superName = "Lfoo/bar/Foo.class"
            ).prefixMatches(prefixes, 1)
        }
    }

}
