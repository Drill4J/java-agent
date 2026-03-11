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

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender

actual object LoggingConfiguration {

    private var filename: String? = null
    private var messageLimit = 512
    private val apiKeyPattern = """\d+_[0-9a-fA-F]{64}""".toRegex()

    actual fun readDefaultConfiguration() {
        PatternLayout.DEFAULT_CONVERTER_MAP["maskedMsg"] = SensitiveDataConverter::class.java.name
        // Temporarily set custom drill-logback.xml configuration file
        // during logger initialization to avoid class loading conflicts with the main application
        val root = withSystemProperty(CONFIG_FILE_PROPERTY, "drill-logback.xml") {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger)
        }
        root.loggerContext.reset()
        root.level = Level.ERROR
        root.addAppender(createConsoleAppender())
    }

    actual fun setLoggingLevels(levels: List<Pair<String, String>>) {
        val levelRegex = Regex("(TRACE|DEBUG|INFO|WARN|ERROR)")
        val isCorrect: (Pair<String, String>) -> Boolean = { levelRegex.matches(it.second) }
        levels.filter(isCorrect).forEach {
            (LoggerFactory.getLogger(it.first) as Logger).level = Level.toLevel(it.second)
        }
    }

    actual fun setLoggingLevels(levels: String) {
        val levelPairRegex = Regex("([\\w.]*=)?(TRACE|DEBUG|INFO|WARN|ERROR)")
        val toLevelPair: (String) -> Pair<String, String>? = { str ->
            str.takeIf(levelPairRegex::matches)?.let { it.substringBefore("=", "ROOT") to it.substringAfter("=") }
        }
        setLoggingLevels(levels.split(";").mapNotNull(toLevelPair))
    }

    actual fun setLoggingFilename(filename: String?) {
        val appender = filename?.runCatching(LoggingConfiguration::createFileAppender)?.getOrNull() ?: createConsoleAppender()
        val withAppenders: (Logger) -> Boolean = { it.iteratorForAppenders().hasNext() }
        (LoggerFactory.getILoggerFactory() as LoggerContext).loggerList.filter(withAppenders).forEach {
            it.detachAndStopAllAppenders()
            it.addAppender(appender)
        }
        LoggingConfiguration.filename = filename
    }

    actual fun getLoggingFilename() = filename

    actual fun setLogMessageLimit(messageLimit: Int) {
        val toAppenders: (Logger) -> Sequence<Appender<ILoggingEvent>> = { it.iteratorForAppenders().asSequence() }
        val toEncoder: (Appender<ILoggingEvent>) -> PatternLayoutEncoder? = {
            (it as? OutputStreamAppender<ILoggingEvent>)?.encoder as? PatternLayoutEncoder
        }
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.loggerList.flatMap(toAppenders).mapNotNull(toEncoder).forEach {
            it.stop()
            it.pattern = "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%logger] %.-${messageLimit}maskedMsg%n%throwable"
            it.start()
        }
        LoggingConfiguration.messageLimit = messageLimit
    }

    actual fun getLogMessageLimit() = messageLimit

    private fun createConsoleAppender() =
        configureOutputStreamAppender(ConsoleAppender()).also(ConsoleAppender<ILoggingEvent>::start)

    private fun createFileAppender(filename: String) = configureOutputStreamAppender(FileAppender()).apply {
        this.file = filename
        this.start()
    }

    private fun <T : OutputStreamAppender<ILoggingEvent>> configureOutputStreamAppender(appender: T) = appender.apply {
        val context = (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).loggerContext
        val encoder = PatternLayoutEncoder().also {
            it.pattern = "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%logger] %.-${messageLimit}maskedMsg%n%throwable"
            it.context = context
            it.start()
        }
        this.context = context
        this.encoder = encoder
    }

    class SensitiveDataConverter : ClassicConverter() {
        override fun convert(event: ILoggingEvent): String {
            return event.formattedMessage.replace(apiKeyPattern, "********")
        }
    }

    private fun <T> withSystemProperty(property: String, value: String, block: () -> T): T {
        val oldValue = System.getProperty(property)
        System.setProperty(property, value)
        try {
            return block()
        } finally {
            oldValue?.let { System.setProperty(property, it) } ?: System.clearProperty(property)
        }
    }
}
