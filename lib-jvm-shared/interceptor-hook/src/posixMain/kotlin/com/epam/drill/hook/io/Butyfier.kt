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
package com.epam.drill.hook.io

import com.epam.drill.hook.gen.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
val nativeRead
    get() = read_func!!

@OptIn(ExperimentalForeignApi::class)
val nativeWrite
    get() = write_func!!

@OptIn(ExperimentalForeignApi::class)
val nativeSend
    get() = send_func!!

@OptIn(ExperimentalForeignApi::class)
val nativeRecv
    get() = recv_func!!

@OptIn(ExperimentalForeignApi::class)
val nativeAccept
    get() = accept_func!!
