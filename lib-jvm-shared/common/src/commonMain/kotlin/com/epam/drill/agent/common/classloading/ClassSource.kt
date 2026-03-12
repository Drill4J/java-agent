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

private const val SUBCLASS_OF = "!subclassOf:"

data class ClassSource(
    private val entityName: String,
    private val superName: String? = null,
    private val bytes: ByteArray = byteArrayOf(),
) : EntitySource {

    override fun entityName() = entityName

    override fun bytes() = bytes

    override fun toString() = "$entityName: ${this::class.simpleName}"

    override fun equals(other: Any?) = other is ClassSource && entityName == other.entityName

    override fun hashCode() = entityName.hashCode()

    fun prefixMatches(prefixes: Iterable<String>, offset: Int = 0): Boolean {
        val isMatchPrefix: () -> Boolean = {
            prefixes.any { entityName.regionMatches(offset, it, 0, it.length) }
        }
        val isNotExcluded: () -> Boolean = {
            prefixes.none { it.startsWith('!') && entityName.regionMatches(offset, it, 1, it.length - 1) }
        }
        val isNotSubclass: () -> Boolean = {
            superName == null || prefixes.none {
                it.startsWith(SUBCLASS_OF) &&
                        superName.isNotBlank() &&
                        superName.regionMatches(offset, it, SUBCLASS_OF.length, it.length - SUBCLASS_OF.length)
            }
        }
        return isMatchPrefix() && isNotExcluded() && isNotSubclass()
    }

}
