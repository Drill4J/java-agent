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
package com.epam.drill.bootstrap

import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import kotlinx.cinterop.*
import platform.posix.*

@CName("Agent_OnLoad")
fun agentOnLoad(
    vmPointer: CPointer<JavaVMVar>,
    options: String,
    reservedPtr: Long
): Int = memScoped {
    try {
        val initialParams = options.asAgentParams()
        loggerCallback = { Logging.logger(it) }
        initialParams["bootstrapConfigPath"]?.let {
            val rawFileContent = readFile(it)
            println(rawFileContent)
            val customAgentParams = rawFileContent.asAgentParams(lineDelimiter = "\n")
            val agentPath = customAgentParams.getValue("agentPath")
            val injectDynamicLibrary = agentLoad(agentPath) as CPointer<*>
            val directOnLoad =
                injectDynamicLibrary.reinterpret<CFunction<(CPointer<JavaVMVar>, CPointer<ByteVar>, Long) -> Int>>()
            val finalOptions = initialParams
                .toMutableMap()
                .apply {
                    put("coreLibPath", agentPath)
                    putAll(customAgentParams)
                }
                .map { (k, v) -> "$k=$v" }.joinToString(separator = ",")
            println(finalOptions)
            directOnLoad(vmPointer, finalOptions.cstr.getPointer(this), reservedPtr)
        } ?: JNI_OK
    } catch (ex: Exception) {
        ex.printStackTrace()
        JNI_OK
    }
}

@CName("Agent_OnUnload")
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>): Unit {
//  com.epam.drill.core.AgentBootstrap.agentOnUnload()
}


private fun String?.asAgentParams(
    lineDelimiter: String = ",",
    filterPrefix: String = "",
    mapDelimiter: String = "="
): Map<String, String> {
    if (this.isNullOrEmpty()) return emptyMap()
    return try {
        this.split(lineDelimiter)
            .filter { it.isNotEmpty() && (filterPrefix.isEmpty() || !it.startsWith(filterPrefix)) }
            .associate {
                val (key, value) = it.split(mapDelimiter)
                val pair = key to value
                pair
            }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $this")
    }
}


private fun readFile(filePath: String): String {
    val fileDescriptor = open(filePath, O_RDONLY)
    if (fileDescriptor == -1) throw IllegalArgumentException("Cannot open the config file with filePath='$filePath'")
    val bytes = Input(fileDescriptor).readBytes()
    return bytes.decodeToString()
}
