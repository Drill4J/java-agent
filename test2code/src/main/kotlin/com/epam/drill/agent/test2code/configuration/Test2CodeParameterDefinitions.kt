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
package com.epam.drill.agent.test2code.configuration

import kotlin.time.Duration
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameterDefinitionCollection
import com.epam.drill.agent.common.configuration.BaseAgentParameterDefinition
import com.epam.drill.agent.common.configuration.ValidationType
import com.epam.drill.agent.configuration.isNotBlank
import com.epam.drill.agent.configuration.minDuration

object Test2CodeParameterDefinitions : AgentParameterDefinitionCollection() {

    val SCAN_CLASS_PATH = AgentParameterDefinition.forList(
        name = "scanClassPath",
        description = "Path to JAR/WAR/EAR to scan",
        defaultValue = emptyList(),
        validation = ValidationType.SOFT,
        itemValidator = {
            isNotBlank()
        }
    ).register()
    val SCAN_CLASS_DELAY = AgentParameterDefinition.forDuration(
        name = "scanClassDelay",
        defaultValue = Duration.ZERO,
        validation = ValidationType.SOFT,
        validator = {
            minDuration(Duration.ZERO)
        }
    ).register()
    val ENABLE_SCAN_CLASS_LOADERS = AgentParameterDefinition.forBoolean(
        name = "enableScanClassLoaders",
        defaultValue = true
    ).register()
    val COVERAGE_SEND_INTERVAL = AgentParameterDefinition.forLong(
        name = "coverageSendInterval",
        defaultValue = 2000L
    ).register()
    val COVERAGE_SEND_PAGE_SIZE = AgentParameterDefinition.forInt(
        name = "coverageSendPageSize",
        defaultValue = 1000
    ).register()
    val METHODS_SEND_PAGE_SIZE = AgentParameterDefinition.forInt(
        name = "methodsSendPageSize",
        defaultValue = 1000
    ).register()
    val COVERAGE_COLLECTION_ENABLED = AgentParameterDefinition.forBoolean(
        name = "coverageCollectionEnabled",
        description = "Enable/disable application code coverage collection",
        defaultValue = true
    ).register()
    val CLASS_SCANNING_ENABLED = AgentParameterDefinition.forBoolean(
        name = "classScanningEnabled",
        description = "Enable/disable application classes scanning",
        defaultValue = true
    ).register()

}
