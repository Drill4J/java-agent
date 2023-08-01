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
package com.epam.drill.agent

import kotlin.native.concurrent.*
import kotlin.time.*

data class Config(
    val classScanDelay: Duration = Duration.ZERO,
    val scanClassPath: String = "",
    val logLevel: String = "ERROR",
    val logFile: String? = null,
    val logLimit: Int = 512,
    val isAsyncApp: Boolean = false,
    val isWebApp: Boolean = false,
    val isKafka: Boolean = false,
    val isCadence: Boolean = false,
    val isTlsApp: Boolean = false
)

private val _config = AtomicReference(Config().freeze()).freeze()

val config: Config get() = _config.value

fun updateConfig(block: Config.() -> Config): Config = _config.value.block().freeze().also {
    _config.value = it
}
