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
package com.epam.drill.agent.test.devtools

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.test.instrument.selenium.*
import com.epam.drill.agent.test.serialization.json
import com.epam.drill.agent.test.session.SessionController
import com.epam.drill.agent.test.transport.TestAgentMessageSender
import mu.KotlinLogging

interface JsCoverageSender {
    fun sendJsCoverage(testLaunchId: String)
}

class JsCoverageSenderImpl : JsCoverageSender {
    private val logger = KotlinLogging.logger {}

    override fun sendJsCoverage(testLaunchId: String) {
        DevToolStorage.get()?.run {
            val coverage = takePreciseCoverage()
            if (coverage.isBlank()) {
                logger.trace { "coverage is blank" }
                return
            }
            val scripts = scriptParsed()
            if (scripts.isBlank()) {
                logger.trace { "script parsed is blank" }
                return
            }
            logger.debug { "ThreadStorage.sendSessionData" }
            sendSessionData(SessionData(coverage, scripts, testLaunchId))
        }
    }

    private fun sendSessionData(data: SessionData) = runCatching {
        val payload = AddSessionData(
            sessionId = SessionController.getSessionId(),
            data = json.encodeToString(SessionData.serializer(), data)
        )
        TestAgentMessageSender.send(
            destination = AgentMessageDestination(
                "POST",
                "raw-javascript-coverage"
            ),
            message = payload,
            serializer = AddSessionData.serializer()
        )
    }.onFailure {
        logger.warn(it) { "can't send js raw coverage ${SessionController.getSessionId()}" }
    }.getOrNull()
}