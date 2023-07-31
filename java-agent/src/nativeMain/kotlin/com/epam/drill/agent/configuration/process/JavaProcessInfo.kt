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
package com.epam.drill.agent.configuration.process

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.EOF
import platform.posix.FILE
import platform.posix.fscanf
import platform.posix.getc
import platform.posix.ungetc
import com.epam.drill.close
import com.epam.drill.openPipe

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

fun javaProcess(): JavaProcess = getProcessInfo().filter(String::isNotBlank).run {
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
        if (chr != ' '.code && chr != EOF) {
            gaps.interrupted = true
        }
        ungetc(chr, pipe)
    }
    if (gaps.spacedString && chunk.last() == '\"') gaps.spacedString = false
    if (chunk.first() == '\"' && chunk.last() != '\"') gaps.spacedString = true
}

private class Gaps(
    var interrupted: Boolean = false,
    var spacedString: Boolean = false
)
