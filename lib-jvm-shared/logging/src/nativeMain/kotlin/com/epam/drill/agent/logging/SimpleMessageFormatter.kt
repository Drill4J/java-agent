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
package com.epam.drill.agent.logging

import kotlin.concurrent.AtomicInt
import io.ktor.util.date.GMTDate
import mu.Formatter
import mu.KotlinLoggingLevel
import mu.Marker
import mu.internal.ErrorMessageProducer
import kotlin.experimental.ExperimentalNativeApi

object SimpleMessageFormatter : Formatter {

    private val _messageLimit = AtomicInt(512)
    var messageLimit: Int
        get() = _messageLimit.value
        set(value) { _messageLimit.value = value }
    private val apiKeyPattern = """\d+_[0-9a-fA-F]{64}""".toRegex()

    override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${msg.toStringSafe()}"

    override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, t: Throwable?, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${msg.toStringSafe()}${t.throwableToString()}"

    override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, marker: Marker?, msg: () -> Any?) =
        "${formatPrefix(level, loggerName)} ${marker.wrapToString()}${msg.toStringSafe()}"

    override fun formatMessage(
        includePrefix: Boolean,
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        t: Throwable?,
        msg: () -> Any?
    ) = "${formatPrefix(level, loggerName)} ${marker.wrapToString()}${msg.toStringSafe()}${t.throwableToString()}"

    private inline fun formatPrefix(level: KotlinLoggingLevel, loggerName: String) =
        "${GMTDate().dateToString()} ${level.name.padEnd(5)} [$loggerName]"

    private inline fun GMTDate.dateToString(): String {
        val padStart: Int.(Int) -> String = { this.toString().padStart(it, '0') }
        return "$year-${month.ordinal.inc().padStart(2)}-${dayOfMonth.padStart(2)} " +
                "${hours.padStart(2)}:${minutes.padStart(2)}:${seconds.padStart(2)}.000"
    }

    private inline fun Marker?.wrapToString(): String {
        val wrapToParentheses: (String) -> String = { "($it) " }
        return this?.let(Marker::getName)?.takeIf(String::isNotEmpty)?.let(wrapToParentheses) ?: ""
    }

    private inline fun (() -> Any?).toStringSafe(): String {
        return try {
            invoke().toString().take(messageLimit).maskSecrets()
        } catch (e: Exception) {
            ErrorMessageProducer.getErrorLog(e)
        }
    }

    @OptIn(ExperimentalNativeApi::class)
    private fun Throwable?.throwableToString(): String {
        if (this == null) return ""
        var msg = "\n"
        var current = this
        while (current != null && current.cause != current) {
            if (current != this) msg += "Caused by: "
            msg += "$current\n"
            current.getStackTrace().forEach { msg += "    at $it\n" }
            current = current.cause
        }
        return msg
    }

    private fun String.maskSecrets(): String {
        return this.replace(apiKeyPattern, "********")
    }

}
