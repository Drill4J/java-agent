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
@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.core.*
import com.epam.drill.core.Agent.isHttpHookEnabled
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.request.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*

private val logger = Logging.logger("VmInitEvent")

@Suppress("UNUSED_PARAMETER")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    initRuntimeIfNeeded()
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    if (isHttpHookEnabled) {
        logger.info { "run with http hook" }
        configureHttp()
    } else {
        logger.warn { "run without http hook" }
    }

    globalCallbacks()
    WsSocket().connect(adminAddress.toString())
    RequestHolder.init(isAsync = config.isAsyncApp)
    runBlocking {
        for (i in 1..5) {
            logger.info { "Agent is not alive. Waiting for package settings from $adminAddress..." }
            delay(500L)
            if (state.alive) {
                logger.info { "Agent is alive! Waiting for loading of at least one plugin..." }
                while (pstorage.none()) {
                    delay(500L)
                }
                logger.info {
                    "At least on plugin is loaded (plugins ${pstorage.keys.toList()}), continue vm initializing."
                }
                break
            }
        }
        if (pstorage.none()) {
            logger.info { "No plugins loaded from $adminAddress." }
        }
    }
}

