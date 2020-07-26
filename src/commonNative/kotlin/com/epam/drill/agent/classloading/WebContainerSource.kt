package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.logger.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

actual object WebContainerSource {

   actual fun webAppStarted(appPath: String) {
        state = state.copy(webApps = (state.webApps + mapOf(appPath to true)))
        logger.info { "App '${appPath}' is initialized" }
    }
}

@SharedImmutable
private val logger = Logging.logger("WebApp")
internal const val waitingTimeout: Long = 1500000 //move to config or admin

internal suspend fun waitForMultipleWebApps() = withTimeoutOrNull(waitingTimeout) {
    while (!state.allWebAppsInitialized()) {
        delay(1500)
        logger.debug { "Waiting for Web app initialization" }
    }
}
