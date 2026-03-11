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

import kotlin.reflect.KProperty
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition
import com.epam.drill.agent.common.configuration.BaseAgentParameterDefinition
import com.epam.drill.agent.common.configuration.ValidationError

expect class DefaultAgentParameters(
    inputParameters: Map<String, String>
) : AgentParameters {
    override operator fun <T : Any> get(name: String): T?
    override operator fun <T : Any> get(definition: AgentParameterDefinition<T>): T
    override operator fun <T : Any> getValue(ref: Any?, property: KProperty<*>): T?
    override fun <T : Any> get(definition: NullableAgentParameterDefinition<T>): T?
    override fun define(vararg definitions: BaseAgentParameterDefinition<out Any>): List<ValidationError<out Any>>
}
