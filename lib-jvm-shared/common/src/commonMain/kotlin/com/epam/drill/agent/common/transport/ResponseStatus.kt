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
package com.epam.drill.agent.common.transport

/**
 * Abstraction for response of any type.
 *
 * It's used in [AgentMessageSender].
 *
 * @see [AgentMessageSender]
 */
class ResponseStatus<T>(
    val success: Boolean,
    val content: T? = null,
    val errorContent: String? = null
) {
    fun onError(block: (String?) -> Unit): ResponseStatus<T> {
        if (!success)
            block(errorContent)
        return this
    }

    fun onSuccess(block: (T?) -> Unit): ResponseStatus<T> {
        if (success)
            block(content)
        return this
    }

    fun <M> mapContent(block: (T) -> M): ResponseStatus<M> {
        return ResponseStatus(
            success = success,
            content = content.takeIf { it != null }?.let { block(it) },
            errorContent = errorContent
        )
    }
}
