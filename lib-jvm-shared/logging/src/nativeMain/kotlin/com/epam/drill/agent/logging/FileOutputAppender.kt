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

import platform.posix.O_CREAT
import platform.posix.O_APPEND
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.open

import io.ktor.utils.io.streams.Output
import mu.Appender

class FileOutputAppender(val filename: String, append: Boolean = true, override val includePrefix: Boolean = true) : Appender {

    private val appendFlag = if (append) O_APPEND else O_TRUNC
    private val openFlags = O_WRONLY or O_CREAT or appendFlag
    private val openMode = S_IWUSR or S_IRUSR or S_IRGRP or S_IROTH
    private val output = Output(open(filename, openFlags, openMode))


    override fun trace(loggerName: String, message: String) = append(message).getOrDefault(Unit)
    override fun debug(loggerName: String, message: String) = append(message).getOrDefault(Unit)
    override fun info(loggerName: String, message: String) = append(message).getOrDefault(Unit)
    override fun warn(loggerName: String, message: String) = append(message).getOrDefault(Unit)
    override fun error(loggerName: String, message: String) = append(message).getOrDefault(Unit)

    fun close() {
        output.close()
    }

    private fun append(message: Any?) = message.toString().runCatching {
        output.appendLine(this)
        output.flush()
    }

}
