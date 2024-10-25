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
package com.epam.drill.test2code.configuration

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.epam.drill.common.agent.configuration.AgentParameterDefinition

object ParameterDefinitions {

    val SCAN_CLASS_PATH = AgentParameterDefinition.forType(
        name = "scanClassPath",
        defaultValue = emptyList(),
        parser = { it.split(";") }
    )
    val SCAN_CLASS_DELAY = AgentParameterDefinition.forType(
        name = "scanClassDelay",
        defaultValue = Duration.ZERO,
        parser = { it.toLong().toDuration(DurationUnit.MILLISECONDS) }
    )
    val ENABLE_SCAN_CLASS_LOADERS = AgentParameterDefinition.forType(
        name = "enableScanClassLoaders",
        defaultValue = true,
        parser = { it.toBoolean() }
    )
    val COVERAGE_SEND_INTERVAL = AgentParameterDefinition.forLong(
        name = "coverageSendInterval",
        defaultValue = 2000L
    )
    val COVERAGE_SEND_PAGE_SIZE = AgentParameterDefinition.forInt(
        name = "coverageSendPageSize",
        defaultValue = 1000
    )
    val METHODS_SEND_PAGE_SIZE = AgentParameterDefinition.forInt(
        name = "methodsSendPageSize",
        defaultValue = 1000
    )

}
