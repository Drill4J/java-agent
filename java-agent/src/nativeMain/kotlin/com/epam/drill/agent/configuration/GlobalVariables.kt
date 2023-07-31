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

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val _agentParameters = AtomicReference(AgentParameters().freeze()).freeze()

@SharedImmutable
val agentStartTimeMark: TimeMark = TimeSource.Monotonic.markNow().freeze()

var agentParameters: AgentParameters
    get() = _agentParameters.value
    set(params) { _agentParameters.value = params.freeze() }
