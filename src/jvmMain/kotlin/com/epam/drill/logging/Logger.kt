/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.logging

import com.epam.drill.logger.*

enum class Level {
    INFO
}

fun log(level: Level, msg: () -> String) {
    println("[DRILL][${level.name}]: ${msg()}")
}

private val trace = Logging.logger("TRACER")

object TraceLog {
    fun log(className: String, methodName: String, action: String) {
        trace.info { "Id, $className:$methodName, $action, ${System.currentTimeMillis()}" }
    }
}
