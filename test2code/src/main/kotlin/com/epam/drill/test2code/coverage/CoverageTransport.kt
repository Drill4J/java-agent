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

import com.epam.drill.agent.websocket.*
import com.epam.drill.common.agent.*
import java.util.concurrent.atomic.*

interface CoverageTransport {
    fun send(message: String)

    fun isAvailable(): Boolean

}

class StubTransport : CoverageTransport {
    override fun send(message: String) {
        throw UnsupportedOperationException()
    }

    override fun isAvailable(): Boolean {
        return false
    }

}

open class WebsocketCoverageTransport(
    private val id: String,
    private val sender: Sender,
    private var isTransportAvailable: AtomicBoolean = AtomicBoolean(false)
) : CoverageTransport {

    init {
        WsClient.setOnAvailable {
            isTransportAvailable.set(true)
        }
        WsClient.setOnUnavailable {
            isTransportAvailable.set(false)
        }
    }

    override fun isAvailable(): Boolean {
        return isTransportAvailable.get()
    }

    override fun send(message: String) {
        sender.send(id, message)
    }
}