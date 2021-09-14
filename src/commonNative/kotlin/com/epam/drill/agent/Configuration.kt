/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.agent.serialization.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.*
import com.epam.drill.interceptor.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.time.*

fun performAgentInitialization(initialParams: AgentParameters) {
    val agentArguments = initialParams.parseAs<AgentArguments>()
    agentArguments.let { aa ->
        drillInstallationDir = aa.drillInstallationDir
        adminAddress = URL("ws://${aa.adminAddress}")
        agentConfig = AgentConfig(
            id = aa.agentId,
            instanceId = aa.instanceId,
            agentVersion = agentVersion,
            buildVersion = aa.buildVersion ?: calculateBuildVersion() ?: "unspecified",
            serviceGroupId = aa.groupId,
            agentType = AGENT_TYPE,
            parameters = aa.defaultParameters(),
        )
        updateConfigs(agentConfig.parameters, initialParams)
        agentConfigUpdater = object : AgentConfigUpdater {
            override fun updateParameters(config: AgentConfig) {
                updateConfigs(config.parameters)
            }
        }
    }
}

fun updateConfigs(parameters: Map<String, AgentParameter>, initialParams: AgentParameters = emptyMap()) {
    parameters[AgentArguments::logLevel.name]?.let {
        Logging.logLevel = LogLevel.valueOf(it.value)
    }
    parameters[AgentArguments::logFile.name]?.let {
        if (Logging.filename != it.value) Logging.filename = it.value
    }

    val newWebApps = parameters[AgentArguments::webAppNames.name]?.value
        ?.takeIf { it.isNotBlank() }?.split(":", ",") ?: emptyList()
    updateConfig {
        copy(
            classScanDelay = parameters[AgentArguments::classScanDelay.name]?.value
                ?.toLong()?.toDuration(DurationUnit.MILLISECONDS) ?: classScanDelay,
            isAsyncApp = parameters[AgentArguments::isAsyncApp.name]?.value.toBoolean(),
            isWebApp = parameters[AgentArguments::isWebApp.name]?.value.toBoolean() || newWebApps.any(),
            isKafka = parameters[AgentArguments::isKafka.name]?.value.toBoolean(),
            isCadence = parameters[AgentArguments::isCadence.name]?.value.toBoolean(),
            isTlsApp = parameters[AgentArguments::isTlsApp.name]?.value.toBoolean(),
            webApps = newWebApps,
            coreLibPath = initialParams["coreLibPath"] ?: coreLibPath
        )
    }

    if (newWebApps.isNotEmpty()) {
        updateState { copy(webApps = newWebApps.associateWith { false }) }
    }
    logger.debug { "after update configs by params: config '$config'; state '$state'" }
}

private fun calculateBuildVersion(): String? = runCatching {
    getenv(SYSTEM_JAVA_APP_JAR)?.toKString()?.let {
        "(.*)/(.*).jar".toRegex().matchEntire(it)?.let { matchResult ->
            if (matchResult.groupValues.size == 3) {
                val buildVersion = matchResult.groupValues[2]
                logger.debug { "calculated build version = '$buildVersion'" }
                buildVersion
            } else null
        }
    }
}.getOrNull()

//TODO remove this code

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
