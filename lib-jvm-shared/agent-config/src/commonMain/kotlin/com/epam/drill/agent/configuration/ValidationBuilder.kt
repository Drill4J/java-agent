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

import com.epam.drill.agent.konform.validation.Constraint
import com.epam.drill.agent.konform.validation.ValidationBuilder
import kotlin.time.Duration

val TRANSPORT_SCHEMES = setOf("http://", "https://")

fun ValidationBuilder<String>.validTransportUrl() = addConstraint(
    "must have a valid URL address, e.g. 'https://localhost:8090', but was '{value}'"
){ TRANSPORT_SCHEMES.any(it::startsWith) }

fun ValidationBuilder<String>.identifier() = addConstraint(
    "must contain only lowercase latin characters",
) {
    it.matches("^[a-z0-9_-]+\$".toRegex())
}

fun ValidationBuilder<String>.isValidPackage(): Constraint<String> = addConstraint(
    "must have a valid Java package delimited by a forward slash, e.g. 'com/example', but was '{value}'"
) { it.matches("^!?[a-zA-Z_]\\w*(?:/[a-zA-Z_]\\w*)*$".toRegex()) }

fun ValidationBuilder<String>.isValidLogLevel(): Constraint<String> = addConstraint(
    "must have a valid logging level for a java package, e.g. 'com.example=INFO', but was '{value}'"
) { it.matches("(([a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*)?=)?(TRACE|DEBUG|INFO|WARN|ERROR)".toRegex()) }

fun ValidationBuilder<Duration>.minDuration(minimumInclusive: Duration) = addConstraint(
    "must be at least '{0}' ms", minimumInclusive.inWholeMilliseconds.toString()
) { it.inWholeMilliseconds >= minimumInclusive.inWholeMilliseconds }

fun ValidationBuilder<String>.isNotBlank(): Constraint<String> = addConstraint(
    "must be not blank, but was {value}",
) { it.isNotBlank() }