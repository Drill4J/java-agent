package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*

@SharedImmutable
private val logger = Logging.logger("WebApp")
internal const val waitingTimeout: Long = 1500000 //move to config or admin

@Suppress("UNUSED_PARAMETER", "unused")
@CName("Java_com_epam_drill_agent_classloading_WebContainerSource_webAppStarted")
fun webAppStarted(env: JNIEnv, thiz: jobject, appPath: jstring) = withJSting {
    val appName = appPath.toKString()
    state = state.copy(webApps = (state.webApps + mapOf(appName to true)))
    logger.info { "App '${appName}' is initialized" }
}


internal suspend fun waitForMultipleWebApps() = withTimeoutOrNull(waitingTimeout) {
    while (!state.allWebAppsInitialized()) {
        delay(1500)
        logger.debug { "Waiting for Web app initialization" }
    }
}
