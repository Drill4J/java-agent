package com.epam.drill.bootstrap

import com.epam.drill.jvmapi.gen.*
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
