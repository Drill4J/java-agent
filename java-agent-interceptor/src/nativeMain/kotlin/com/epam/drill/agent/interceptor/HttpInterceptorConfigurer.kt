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
package com.epam.drill.agent.interceptor

import kotlin.native.concurrent.freeze
import mu.KotlinLogging
import com.epam.drill.common.agent.request.HeadersRetriever
import com.epam.drill.common.agent.request.RequestHolder
import com.epam.drill.hook.io.addInterceptor
import com.epam.drill.hook.io.configureTcpHooks
import com.epam.drill.hook.io.injectedHeaders
import com.epam.drill.hook.io.readHeaders
import com.epam.drill.hook.io.writeCallback

@Suppress("unused")
object HttpInterceptorConfigurer {

    private val logger = KotlinLogging.logger("com.epam.drill.agent.interceptor.HttpInterceptorConfigurer")

    const val enabled = true

    operator fun invoke(headersRetriever: HeadersRetriever, requestHolder: RequestHolder) {
        logger.debug { "invoke: Creating HTTP interceptor object..." }
        val interceptor = HttpInterceptor()

        logger.debug { "invoke: Creating HTTP interceptor callbacks..." }
        val callbacks = HttpInterceptorCallbacks(headersRetriever, requestHolder)

        logger.debug { "invoke: Configuring TCP hook..." }
        configureTcpHooks()
        addInterceptor(interceptor.freeze())

        logger.debug { "invoke: Configuring TCP hook callbacks..." }
        injectedHeaders.value = callbacks::injectedHeaders.freeze()
        readHeaders.value = callbacks::readHeaders.freeze()
        writeCallback.value = callbacks::writeCallback.freeze()
    }

}
