package com.epam.drill.agent.classloading

import com.epam.drill.agent.*
import com.epam.drill.jvmapi.gen.*
import mu.*
import kotlin.native.concurrent.*

@SharedImmutable
private val logger = KotlinLogging.logger("WebApp")


@Suppress("UNUSED_PARAMETER", "unused")
@CName("Java_com_epam_drill_agent_classloading_WebContainerSource_webAppStarted")
fun webAppStarted(env: JNIEnv, thiz: jobject, appPath: jstring) {
    state = state.copy(isWebAppInitialized = true)
    logger.info { "App '${appPath}' is initialized" }
}