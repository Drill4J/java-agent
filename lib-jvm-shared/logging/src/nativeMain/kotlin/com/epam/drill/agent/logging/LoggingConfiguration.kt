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

import mu.ConsoleOutputAppender
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import kotlin.native.concurrent.freeze

actual object LoggingConfiguration {

    actual fun readDefaultConfiguration() {
        KotlinLoggingConfiguration.formatter = SimpleMessageFormatter
        KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.INFO
    }

    actual fun setLoggingLevels(levels: List<Pair<String, String>>) {
        val levelRegex = Regex("(TRACE|DEBUG|INFO|WARN|ERROR)")
        val defaultLoggers = sequenceOf("", "com", "com.epam", "com.epam.drill")
        val isCorrect: (Pair<String, String>) -> Boolean = { levelRegex.matches(it.second) }
        val isDefaultLogger: (Pair<String, String>) -> Boolean = { defaultLoggers.contains(it.first) }
        levels.filter(isCorrect).sortedBy(Pair<String, String>::first).lastOrNull(isDefaultLogger)?.let {
            KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.valueOf(it.second)
        }
    }

    actual fun setLoggingLevels(levels: String) {
        val levelPairRegex = Regex("([\\w.]*=)?(TRACE|DEBUG|INFO|WARN|ERROR)")
        val toLevelPair: (String) -> Pair<String, String>? = { str ->
            str.takeIf(levelPairRegex::matches)?.let { it.substringBefore("=", "") to it.substringAfter("=") }
        }
        setLoggingLevels(levels.split(";").mapNotNull(toLevelPair))
    }

    actual fun setLoggingFilename(filename: String?) {
        (KotlinLoggingConfiguration.appender as? FileOutputAppender)?.runCatching(FileOutputAppender::close)
        KotlinLoggingConfiguration.appender =
            filename?.runCatching(::FileOutputAppender)?.getOrNull()?.freeze() ?: ConsoleOutputAppender.freeze()
    }

    actual fun getLoggingFilename() = (KotlinLoggingConfiguration.appender as? FileOutputAppender)?.filename

    actual fun setLogMessageLimit(messageLimit: Int) {
        SimpleMessageFormatter.messageLimit = messageLimit.freeze()
    }

    actual fun getLogMessageLimit() = SimpleMessageFormatter.messageLimit

}
