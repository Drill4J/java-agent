/**
 * Copyright 2020 EPAM Systems
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

fun String.matches(others: Iterable<String>, thisOffset: Int = 0): Boolean = others.any {
    regionMatches(thisOffset, it, 0, it.length)
} && others.none {
    it.startsWith('!') && regionMatches(thisOffset, it, 1, it.length - 1)
}
