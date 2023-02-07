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
package com.epam.drill.agent

import com.epam.drill.agent.classloading.source.*
import kotlin.test.*

class StringsTest {
    @Test
    fun `prefix matching`() {
        val prefixes = listOf("foo/bar", "!foo/bar/Bar", "!subclassOf:foo/bar/Foo")
        assertTrue { "foo/bar/Baz.class".toClassSource().matches(prefixes) }
        assertTrue { "Lfoo/bar/Baz.class".toClassSource().matches(prefixes, 1) }
        assertFalse { "foo/baz/Baz.class".toClassSource().matches(prefixes) }
        assertFalse { "Lfoo/baz/Baz.class".toClassSource().matches(prefixes, 1) }
        assertFalse { "foo/bar/Bar.class".toClassSource().matches(prefixes) }
        assertFalse { "Lfoo/bar/Bar.class".toClassSource().matches(prefixes, 1) }
        assertFalse {
            ClassSource(
                className = "foo/bar/Baz.class",
                superName = "foo/bar/Foo.class"
            ).matches(prefixes)
        }
        assertFalse {
            ClassSource(
                className = "Lfoo/bar/Baz.class",
                superName = "Lfoo/bar/Foo.class"
            ).matches(prefixes, 1)
        }
    }

}
