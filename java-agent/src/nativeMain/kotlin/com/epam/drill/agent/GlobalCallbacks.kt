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

import com.epam.drill.agent.request.*
import kotlin.native.concurrent.*

private val drillRequestCallback = AtomicReference<() -> DrillRequest?>({ null }.freeze()).freeze()
private val sessionStorageCallback = AtomicReference({ _: DrillRequest -> }.freeze()).freeze()
private val closeSessionCallback = AtomicReference({ }.freeze()).freeze()

var drillRequest: () -> DrillRequest?
    get() = drillRequestCallback.value
    set(value) {
        drillRequestCallback.value = value.freeze()
    }

var sessionStorage: (DrillRequest) -> Unit
    get() = sessionStorageCallback.value
    set(value) {
        sessionStorageCallback.value = value.freeze()
    }

var closeSession: () -> Unit
    get() = closeSessionCallback.value
    set(value) {
        closeSessionCallback.value = value.freeze()
    }
