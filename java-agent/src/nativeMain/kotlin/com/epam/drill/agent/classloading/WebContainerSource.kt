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
package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.core.time.*
import com.epam.drill.logger.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

actual object WebContainerSource {

    actual fun webAppStarted(appPath: String) {
        updateState { copy(webApps = (state.webApps + mapOf(appPath to true))) }
        logger.info { "App '${appPath}' is initialized" }
    }
}

@SharedImmutable
private val logger = Logging.logger("WebApp")

internal suspend fun waitForMultipleWebApps(duration: Duration) = withTimeoutOrNull(duration) {
    while (!state.allWebAppsInitialized()) {
        delay(1500)
        logger.debug { "Waiting for Web app initialization" }
    }
}
