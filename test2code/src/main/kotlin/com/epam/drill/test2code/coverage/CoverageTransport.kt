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
package com.epam.drill.test2code.coverage

import kotlin.concurrent.*
import java.util.concurrent.atomic.*
import com.epam.drill.common.agent.transport.*
import com.epam.drill.plugins.test2code.common.api.*
import com.epam.drill.plugins.test2code.common.transport.*

interface CoverageTransport {
    fun send(message: List<ExecClassData>)
    fun isAvailable(): Boolean
}

class StubTransport : CoverageTransport {
    override fun send(message: List<ExecClassData>) = Unit
    override fun isAvailable(): Boolean = false
}

open class HttpCoverageTransport(
    private val sender: AgentMessageSender,
) : CoverageTransport {

    private val destination = AgentMessageDestination("POST", "coverage-data")
    private var available: AtomicBoolean = AtomicBoolean(false)

    init {
        waitTransportAvailable()
    }

    override fun isAvailable() = available.get()

    override fun send(message: List<ExecClassData>) {
        val sendMessage: (List<ExecClassData>) -> Unit = {
            sender.send(destination, CoverageData(message))
        }
        val markUnavailable: (Throwable) -> Unit = {
            available.takeIf(AtomicBoolean::get)?.set(false)?.also { waitTransportAvailable() }
        }
        message.runCatching(sendMessage).onFailure(markUnavailable).getOrThrow()
    }

    private fun waitTransportAvailable() = thread {
        while(!sender.isTransportAvailable() || emptyList<ExecClassData>().runCatching(::send).isFailure) {
            Thread.sleep(500)
        }
        available.set(true)
    }

}
