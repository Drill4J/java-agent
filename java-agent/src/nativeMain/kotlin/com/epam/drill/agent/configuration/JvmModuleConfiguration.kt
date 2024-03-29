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

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import com.epam.drill.agent.Agent

object JvmModuleConfiguration {

    private val logger = KotlinLogging.logger("com.epam.drill.agent.configuration.JvmModuleConfiguration")

    fun getPackagePrefixes(): String = agentConfig.packagesPrefixes.packagesPrefixes.joinToString(";")

    fun getScanClassPath(): String = agentParameters.scanClassPath

    fun getCoverageRetentionLimit(): String = agentParameters.coverageRetentionLimit

    fun getSendCoverageInterval(): Long = agentParameters.sendCoverageInterval

    fun waitClassScanning() = runBlocking {
        val classScanDelay = agentParameters.classScanDelay - Agent.startTimeMark.elapsedNow()
        if (classScanDelay.isPositive()) {
            logger.debug { "Waiting class scan delay ($classScanDelay left)..." }
            delay(classScanDelay)
        }
    }

}
