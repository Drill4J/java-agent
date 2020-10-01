package com.epam.drill.agent

import kotlin.native.concurrent.*
import kotlin.time.*

data class Config(
    val classScanDelay: Duration = Duration.ZERO,
    val isAsyncApp: Boolean = false,
    val isWebApp: Boolean = false,
    val isTlsApp: Boolean = false,
    val webAppLoadingTimeout: Duration = 1500.seconds,
    val webApps: List<String> = emptyList()
)

private val _config = AtomicReference(Config().freeze()).freeze()

val config: Config get() = _config.value

fun updateConfig(block: Config.() -> Config): Config = _config.value.block().freeze().also {
    _config.value = it
}
