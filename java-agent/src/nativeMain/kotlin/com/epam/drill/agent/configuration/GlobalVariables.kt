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

import kotlin.native.concurrent.*
import com.epam.drill.common.agent.configuration.*

private val _requestPattern = AtomicReference<String?>(null).freeze()
private val _adminAddress = AtomicReference<URL?>(null).freeze()
private val _agentConfig = AtomicReference<AgentConfig?>(null).freeze()
private val _agentParameters = AtomicReference(AgentParameters().freeze()).freeze()

//val drillInstallationDir: String = drillInstallationDir()

var requestPattern: String?
    get() = _requestPattern.value
    set(value) {
        _requestPattern.value = value.freeze()
    }

var adminAddress: URL?
    get() = _adminAddress.value
    set(value) {
        _adminAddress.value = value.freeze()
    }

var agentConfig: AgentConfig
    get() = _agentConfig.value!!
    set(value) {
        _agentConfig.value = value.freeze()
    }

var agentParameters: AgentParameters
    get() = _agentParameters.value
    set(params) {
        _agentParameters.value = params.freeze()
    }
