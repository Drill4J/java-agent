package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.logger.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import kotlin.time.*

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
