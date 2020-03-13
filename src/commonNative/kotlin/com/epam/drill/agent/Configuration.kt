package com.epam.drill.agent

import com.benasher44.uuid.*
import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import kotlinx.cinterop.*
import platform.posix.*

fun performAgentInitialization(initialParams: Map<String, String>) {
    val adminAddress = initialParams["adminAddress"] ?: throw RuntimeException("")
    val agentId = initialParams["agentId"] ?: throw RuntimeException("")
    val buildVersion = initialParams["buildVersion"] ?: "unspecified"
    val instanceId = initialParams["instanceId"] ?: uuid4().toString()
    val groupId = initialParams["groupId"] ?: initialParams["serviceGroupId"] ?: ""
    initialParams["type"]?.let {
        state = when (enumValueOf<ApplicationType>(it)) {
            ApplicationType.WAR -> state.copy(isWebAppInitialized = false)
            ApplicationType.EAR -> state.copy(isWebAppInitialized = false)
        }
    }
    val drillInstallationDir = initialParams["drillInstallationDir"] ?: javaProcess().firstAgentPath
    exec {
        this.drillInstallationDir = drillInstallationDir
        this.agentConfig = AgentConfig(agentId, instanceId, buildVersion, groupId, AGENT_TYPE)
        this.adminAddress = URL("ws://$adminAddress")
    }
}

data class JavaProcess(
    val javaAgents: MutableList<String> = mutableListOf(),
    val nativeAgents: MutableList<String> = mutableListOf(),
    var processPath: String = "",
    var classpath: String = "",
    var jar: String = "",
    var javaParams: List<String>? = null
) {
    val firstAgentPath
        get() = nativeAgents
            .first()
            .split("=")
            .first()
            .replace('/', '\\')
            .substringBeforeLast('\\')
}

fun javaProcess(): JavaProcess = getProcessInfo()
    .filter { it.isNotBlank() }
    .run {
        val javaProcess = JavaProcess()
        val message = this.groupBy { it.startsWith("-D") }
        javaProcess.javaParams = message[true]
        val list = message.getValue(false).iterator()
        javaProcess.processPath = list.next()
        while (list.hasNext()) {
            val next = list.next()
            when {
                next == "-cp" -> {
                    javaProcess.classpath = list.next()
                }
                next == "-jar" -> {
                    javaProcess.jar = list.next()
                }
                next.startsWith("-agentpath") -> {
                    javaProcess.nativeAgents.add(next.replace("-agentpath:", ""))
                }
                next.startsWith("-javaagent") -> {
                    javaProcess.javaAgents.add(next.replace("-javaagent:", ""))
                }
            }
        }
        println(javaProcess)
        javaProcess
    }


fun getProcessInfo(bufferSize: Int = 128): List<String> = memScoped {
    val buffer = " ".repeat(bufferSize).cstr.getPointer(this)
    val result = mutableListOf<String>()
    val pipe: CPointer<FILE>? = openPipe()
    val gaps = Gaps()
    while (fscanf(pipe, "%${bufferSize}s", buffer) == 1) {
        val chunk = processNextWord(buffer, gaps, result)
        checkGaps(gaps, chunk, bufferSize, pipe)
    }
    pipe.close()
    result
}

private fun processNextWord(
    buffer: CPointer<ByteVar>,
    gaps: Gaps,
    result: MutableList<String>
): String {
    val chunk = buffer.toKString()
    if (gaps.interrupted || gaps.spacedString) {
        val replacement = (result.last()) + chunk
        result.removeAt(result.lastIndex)
        result.add(replacement)
    } else {
        result.add(chunk)
    }
    return chunk
}

private fun checkGaps(
    gaps: Gaps,
    chunk: String,
    bufferSize: Int,
    pipe: CPointer<FILE>?
) {
    gaps.interrupted = false
    if (chunk.length == bufferSize) {
        val chr = getc(pipe)
        if (chr != ' '.toInt() && chr != EOF) {
            gaps.interrupted = true
        }
        ungetc(chr, pipe)
    }
    if (gaps.spacedString && chunk.last() == '\"') gaps.spacedString = false
    if (chunk.first() == '\"' && chunk.last() != '\"') gaps.spacedString = true
}

class Gaps(
    var interrupted: Boolean = false,
    var spacedString: Boolean = false
)