/**
 * Copyright 2020 - 2023 EPAM Systems
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
package com.epam.drill.agent.configuration

import com.epam.drill.konform.validation.Constraint
import com.epam.drill.konform.validation.ValidationBuilder
import com.epam.drill.transport.URL
import platform.posix.access
import platform.posix.F_OK

fun ValidationBuilder<String>.hostAndPort(): Constraint<String> {
    return addConstraint(
        "must have a valid URL address without schema and any additional paths, e.g. localhost:8090"
    ) {
        try {
            URL("ws://$it")
            true
        } catch (parseException: RuntimeException) {
            false
        }
    }
}

fun ValidationBuilder<String>.identifier(): Constraint<String> = addConstraint(
    "must contain only lowercase characters, dashes, and underscores",
    ) { it.matches("^[a-z0-9_-]+\$".toRegex()) }


fun ValidationBuilder<String>.pathExists(): Constraint<String> = addConstraint(
    "must have a valid file path",
) { pathExists(it) }


private fun pathExists(filePath: String): Boolean {
    return access(filePath, F_OK) == 0
}