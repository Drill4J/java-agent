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
package com.epam.drill.agent.configuration

import com.epam.drill.konform.validation.Constraint
import com.epam.drill.konform.validation.ValidationBuilder
import platform.posix.access
import platform.posix.F_OK

val WS_SCHEMES = setOf("http", "https")

fun ValidationBuilder<String>.validWsUrl(): Constraint<String> {
    return addConstraint(
        "must have a valid WebSocket URL address, e.g. 'wss://localhost:8090', but was '{value}'"
    ) {
        try {
            val url = URL(it)
            WS_SCHEMES.contains(url.scheme)
        } catch (parseException: RuntimeException) {
            false
        }
    }
}

fun ValidationBuilder<String>.identifier(): Constraint<String> = addConstraint(
    "must contain only lowercase latin characters",
) { it.matches("^[a-z0-9_-]+\$".toRegex()) }


fun ValidationBuilder<String>.pathExists(): Constraint<String> = addConstraint(
    "must be an existing filepath, but was {value}",
) { pathExists(it) }

fun ValidationBuilder<String>.isValidPackage(): Constraint<String> = addConstraint(
    "must have a valid Java package delimited by a forward slash, e.g. 'com/example', but was '{value}'"
) { it.matches("(?:[a-zA-Z_]\\w*(?:/[a-zA-Z_]\\w*)*)?".toRegex()) }

fun ValidationBuilder<String>.isValidLogLevel(): Constraint<String> = addConstraint(
    "must have a valid logging level for a java package, e.g. 'com.example=INFO', but was '{value}'"
) { it.matches("(([a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*)?=)?(TRACE|DEBUG|INFO|WARN|ERROR)".toRegex()) }

private fun pathExists(filePath: String): Boolean {
    return access(filePath, F_OK) == 0
}