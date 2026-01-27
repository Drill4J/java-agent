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
package com.epam.drill.agent.test

import com.epam.drill.agent.test.instrument.selenium.*
import com.epam.drill.agent.test.session.SessionController
import com.epam.drill.agent.test.execution.TestController
import kotlin.jvm.java

const val TEST_ID_HEADER = "drill-test-id"
const val SESSION_ID_HEADER = "drill-session-id"

val TEST_NAME_VALUE_CALC_LINE = "((String)${TestController::class.qualifiedName}.INSTANCE.${TestController::getTestLaunchId.name}())"
val TEST_NAME_CALC_LINE = "\"$TEST_ID_HEADER\", $TEST_NAME_VALUE_CALC_LINE"
val SESSION_ID_VALUE_CALC_LINE = "${SessionController::class.qualifiedName}.INSTANCE.${SessionController::getSessionId.name}()"
val SESSION_ID_CALC_LINE = "\"$SESSION_ID_HEADER\", $SESSION_ID_VALUE_CALC_LINE"
val IF_CONDITION = "$TEST_NAME_VALUE_CALC_LINE != null && $SESSION_ID_VALUE_CALC_LINE != null"
val DEV_TOOL = "((${ChromeDevTool::class.java.name})${DevToolStorage::class.java.name}.INSTANCE.${DevToolStorage::get.name}())"
val IS_DEV_TOOL_NOT_NULL = "$DEV_TOOL != null"
val IS_HEADER_ADDED = "($DEV_TOOL != null && $DEV_TOOL.${ChromeDevTool::isHeadersAdded.name}())"
val ARE_DRILL_HEADERS_PRESENT = "$TEST_NAME_VALUE_CALC_LINE != null && $SESSION_ID_VALUE_CALC_LINE != null"
