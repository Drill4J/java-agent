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
package com.epam.drill.agent.instrument.reactor

import java.util.concurrent.Callable
import mu.KotlinLogging
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder

private val logger = KotlinLogging.logger {}

/**
 * The Callable that propagates the Drill Request.
 * @param drillRequest the value of the Drill Request
 * @param decorate the Callable to be executed with the Drill Request
 */
class PropagatedDrillRequestCallable<T>(
    private val drillRequest: DrillRequest,
    private val requestHolder: RequestHolder,
    private val decorate: Callable<T>
) : Callable<T> {
    override fun call() = propagateDrillRequest<T>(drillRequest, requestHolder) {
        logger.trace { "Scheduled task ran, sessionId = ${drillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
        val result = decorate.call()
        logger.trace { "Scheduled task finished, sessionId = ${drillRequest.drillSessionId}, threadId = ${Thread.currentThread().id}" }
        result
    }
}
