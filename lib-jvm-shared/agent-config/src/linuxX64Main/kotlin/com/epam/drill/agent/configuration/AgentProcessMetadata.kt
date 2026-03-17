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

import kotlinx.cinterop.memScoped
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open
import platform.posix.pclose
import platform.posix.popen
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input
import kotlinx.cinterop.ExperimentalForeignApi

actual object AgentProcessMetadata {

    actual val commandLine: String
        get() = commandLine()

    actual val environmentVars: Map<String, String>
        get() = environmentVars()

    @OptIn(ExperimentalForeignApi::class)
    private fun commandLine() = memScoped {
        val file = open("/proc/self/cmdline", O_RDONLY)
        Input(file).readText().also { close(file) }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun environmentVars() = memScoped {
        val file = popen("env", "r")!!
        Input(file).readText().also { pclose(file) }
            .lines().filter(String::isNotEmpty)
            .associate { it.substringBefore("=") to it.substringAfter("=", "") }
    }

}
