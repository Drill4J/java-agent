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
package com.epam.drill.agent.test.instrument.selenium

import com.epam.drill.agent.test.*
import com.epam.drill.agent.test.serialization.*
import com.epam.drill.agent.test.session.*
import com.epam.drill.agent.common.transport.AgentMessage
import com.epam.drill.agent.common.transport.ResponseStatus
import com.epam.drill.agent.configuration.Configuration
import com.epam.drill.agent.configuration.ParameterDefinitions
import com.epam.drill.agent.test.instrument.TestSessionHeadersProcessor
import com.epam.drill.agent.test.devtools.DevToolsMessageSender
import com.epam.drill.agent.test.serialization.json
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import java.net.*
import java.util.*
import mu.KotlinLogging
import kotlin.reflect.KClass

private const val DEBUGGER_ADDRESS = "debuggerAddress"
private const val DEV_TOOL_DEBUGGER_URL = "webSocketDebuggerUrl"
private val JAVA_TOGGLES = listOf("Network")
private val JS_TOGGLES = listOf("Debugger", "Profiler")
    .takeIf { Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE] }
    ?: emptyList()
private val REPLACE_LOCALHOST = Configuration.parameters[ParameterDefinitions.DEVTOOLS_REPLACE_LOCALHOST]

/**
 * Works with local or Selenoid DevTools by websocket
 */
class ChromeDevTool(
    private val capabilities: Map<*, *>?,
    private val remoteHost: String?
) {
    private val logger = KotlinLogging.logger {}
    private val launchType = Configuration.parameters[ParameterDefinitions.LAUNCH_TYPE]
    private var isClosed = false

    private lateinit var targetUrl: String
    private lateinit var targetId: String
    private var sessionId: SessionId = SessionId()
    private var headersAdded: Boolean = false

    /**
     * connect to remote Selenoid or local webDriver
     */
    fun connect(browserSessionId: String?, currentUrl: String) = runCatching {
        logger.debug { "starting connectToDevTools with cap='$capabilities' sessionId='$sessionId' remote='$remoteHost'..." }
        retrieveDevToolAddress(capabilities ?: emptyMap<String, Any>(), browserSessionId, remoteHost).let {
            trackTime("connect to devtools") {
                connect(it, currentUrl)
            }
        }
        /**
         * Add this to thread local only if successfully connected
         */
    }.onFailure { logger.warn(it) { "UI coverage will be lost. Reason: " } }.getOrNull()

    fun addHeaders(headers: Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        val casted = headers as Map<String, String>
        try {
            logger.trace { "try to add headers: $headers" }
            val success = setHeaders(casted)
            logger.debug { "Chrome Tool activated: ${sessionId.sessionId.isNotBlank()}. Headers: $headers" }
            if (!success) throw RuntimeException("Can't add headers: $headers")
        } catch (ex: Exception) {
            logger.debug { "exception $ex; try to resend" }
            Thread.sleep(2000)
            setHeaders(casted)
        }
    }

    fun switchSession(url: String) {
        val targetId = retrieveTargetId(url)
        logger.trace { "Reconnect to target: $targetId, sessionId: ${sessionId.sessionId}, url $url" }
        targetId?.takeIf { it != this.targetId }?.let { attachToTarget(it) }?.let {
            this.targetId = targetId
            sessionId = it
            enableToggles()
            startCollectJsCoverage()
        }
    }

    fun isHeadersAdded(): Boolean = this.headersAdded

    private fun startCollectJsCoverage() =
        Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE].takeIf(true::equals)?.let {
            disableCache() && startPreciseCoverage() && enableScriptParsed()
        }?.also { success ->
            if (!success) logger.warn { "JS coverage may be lost" }
        }

    private fun startPreciseCoverage() = mapOf("detailed" to true, "callCount" to false).let { params ->
        executeCommand(
            "Profiler.startPreciseCoverage",
            DevToolsRequest(targetUrl, sessionId.sessionId, params.toOutput())
        ).success
    }

    private fun disableCache() = executeCommand(
        "Network.setCacheDisabled",
        DevToolsRequest(targetUrl, sessionId.sessionId, mapOf("cacheDisabled" to true).toOutput())
    ).success

    private fun enableScriptParsed() = DevToolsMessageSender.send(
        "POST",
        "/event/Debugger.scriptParsed",
        DevToolsRequest(targetUrl, sessionId.sessionId)
    ).success

    fun takePreciseCoverage(): String = executeCommand(
        "Profiler.takePreciseCoverage",
        DevToolsRequest(targetUrl, sessionId.sessionId)
    ).takeIf(ResponseStatus<String>::success)?.content ?: ""

    fun scriptParsed(): String = DevToolsMessageSender.send(
        "POST",
        "/event/Debugger.scriptParsed/get-data",
        DevToolsRequest(targetUrl, sessionId.sessionId)
    ).takeIf(ResponseStatus<String>::success)?.content ?: ""

    fun close() {
        if (!isClosed) {
            disableToggles()
            stopCollectJsCoverage()
            DevToolsMessageSender.send(
                "DELETE",
                "/connection",
                DevToolsRequest(targetUrl)
            )
        }
        isClosed = true
    }

    private fun stopCollectJsCoverage() =
        Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE].takeIf(true::equals)?.let {
            DevToolsMessageSender.send(
                "DELETE",
                "/event/Debugger.scriptParsed",
                DevToolsRequest(targetUrl, sessionId.sessionId)
            )
        }

    // todo is it necessary to disable toggles when browser exit?
    private fun disableToggles() = (JAVA_TOGGLES + JS_TOGGLES).map {
        executeCommand(
            "$it.disable",
            DevToolsRequest(targetUrl, sessionId.sessionId)
        ).success
    }.all { it }

    private fun enableToggles() = (JAVA_TOGGLES + JS_TOGGLES).map {
        executeCommand(
            "$it.enable",
            DevToolsRequest(targetUrl, sessionId.sessionId)
        ).success
    }.all { it }.also {
        if (!it) logger.warn { "Toggles wasn't enable" } else logger.info { "Toggles enabled" }
    }

    private fun retrieveDevToolAddress(
        capabilities: Map<*, *>,
        sessionId: String?,
        remoteHost: String?
    ): String = when (launchType) {

        // selenoid provides no access to /json/version, but allows to connect to debugger directly
        // see https://aerokube.com/selenoid/latest/#_accessing_browser_developer_tools
        "selenoid" -> {
            if (sessionId.isNullOrBlank())
                throw RuntimeException("Can't connect to debugger directly, because 'sessionId' is null")
            if (remoteHost.isNullOrBlank())
                throw RuntimeException("Can't connect to debugger directly, because 'remoteHost' is null")
            "ws://$remoteHost/devtools/$sessionId"
        }

        else -> capabilities.run {
            val debuggerURL = get(DEBUGGER_ADDRESS)?.toString()

            if (debuggerURL.isNullOrBlank()) {
                error("Can't get debugger address by field name $DEBUGGER_ADDRESS from capabilities: $capabilities}")
            }

            return DevToolsMessageSender.send("http://$debuggerURL", "GET", "/json/version", "")
                .onError { error ->
                    error("Can't get debugger address from http://$debuggerURL/json/version: $error")
                }
                .onSuccess { content ->
                    logger.trace { "/json/version: $content" }

                }.content?.let { content ->
                    val chromeInfo = Json.parseToJsonElement(content) as JsonObject
                    chromeInfo[DEV_TOOL_DEBUGGER_URL]?.jsonPrimitive?.contentOrNull
                }
                ?: error("Can't get debugger address from '$DEV_TOOL_DEBUGGER_URL' field")
        }
    }


    private fun connect(devToolAddress: String, currentUrl: String) {
        if (REPLACE_LOCALHOST.isNotBlank()) {
            targetUrl = devToolAddress.replace("localhost", REPLACE_LOCALHOST)
        } else {
            targetUrl = devToolAddress
        }
        val success: Boolean = connectToDevTools().takeIf { it }?.also {
            val targetId = retrieveTargetId(currentUrl)
            logger.info { "Retrieved target for url $currentUrl: $targetId" }
            targetId?.let { attachToTarget(it) }?.also {
                this.targetId = targetId
                sessionId = it
                logger.debug { "DevTools session created: $sessionId" }
                enableToggles()
                startCollectJsCoverage()
            }
        } ?: false
        if (success) {
            DevToolStorage.set(this)
        } else throw RuntimeException("Can't connect to $targetUrl")
    }

    private fun connectToDevTools(): Boolean {
        logger.debug { "DevTools URL: $targetUrl" }
        val response = DevToolsMessageSender.send(
            "POST",
            "/connection",
            DevToolsRequest(targetUrl)
        )
        return response.success
    }

    fun startIntercept(): Boolean {
        val headers = TestSessionHeadersProcessor.retrieveHeaders()
        if (headers.isEmpty()) return false
        logger.debug { "Start intercepting. Headers: $headers, sessionId: $sessionId" }
        val response = DevToolsMessageSender.send(
            "POST",
            "/intercept",
            DevToolInterceptRequest(targetUrl, params = mapOf("headers" to headers))
        )
        return response.success
    }


    fun stopIntercept(): Boolean {
        logger.debug { "Stop intercepting: $targetUrl, sessionId $sessionId" }
        val response = DevToolsMessageSender.send(
            "DELETE",
            "/intercept",
            DevToolInterceptRequest(targetUrl)
        )
        setHeaders(mapOf())
        return response.success
    }

    private fun setHeaders(
        params: Map<String, String>
    ): Boolean = executeCommand(
        "Network.setExtraHTTPHeaders",
        DevToolsHeaderRequest(targetUrl, sessionId.sessionId, mapOf("headers" to params))
    ).success

    @Deprecated(message = "Useless")
    private fun autoAttach(): Boolean {
        val params = mapOf("autoAttach" to true, "waitForDebuggerOnStart" to false).toOutput()
        return executeCommand(
            "Target.setAutoAttach",
            DevToolsRequest(targetUrl, sessionId.sessionId, params = params)
        ).success
    }

    private fun attachToTarget(targetId: String): SessionId? {
        val params = mapOf("targetId" to targetId, "flatten" to true).toOutput()
        val response = executeCommand(
            "Target.attachToTarget",
            DevToolsRequest(target = targetUrl, params = params),
            SessionId::class
        )
        return response.takeIf(ResponseStatus<SessionId>::success)
            ?.let(ResponseStatus<SessionId>::content)
    }

    private fun retrieveTargetId(currentUrl: String): String? = targets()
        .find { it.url == currentUrl }
        ?.targetId
        ?.uppercase(Locale.getDefault())
        ?.takeIf { it.isNotBlank() }

    private fun targets(): List<Target> = executeCommand(
        "Target.getTargets",
        DevToolsRequest(target = targetUrl),
        TargetInfos::class
    ).takeIf(ResponseStatus<TargetInfos>::success)
        ?.let(ResponseStatus<TargetInfos>::content)
        ?.let(TargetInfos::targetInfos)
        ?: emptyList()

    private fun executeCommand(
        commandName: String,
        request: DevToolsMessage,
        httpMethod: String = "POST"
    ): ResponseStatus<String> = DevToolsMessageSender.send(httpMethod, "/command/$commandName", request)

    private fun <T : AgentMessage> executeCommand(
        commandName: String,
        request: DevToolsMessage,
        clazz: KClass<T>,
        httpMethod: String = "POST"
    ): ResponseStatus<T> = DevToolsMessageSender.send(httpMethod, "/command/$commandName", request, clazz)

    private fun Map<String, Any>.toOutput(): Map<String, JsonElement> = mapValues { (_, value) ->
        val serializer = value::class.serializer().cast()
        json.encodeToJsonElement(serializer, value)
    }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> KSerializer<out T>.cast(): KSerializer<T> = this as KSerializer<T>

    private inline fun <T> trackTime(tag: String = "", debug: Boolean = false, block: () -> T) =
        measureTimedValue { block() }.apply {
            val logger = KotlinLogging.logger {}
            val message = "[$tag] took: $duration"
            when {
                duration.toDouble(DurationUnit.SECONDS) > 1 -> {
                    logger.warn { message }
                }

                duration.toDouble(DurationUnit.SECONDS) > 30 -> {
                    logger.error { message }
                }

                else -> if (debug) logger.debug { message } else logger.trace { message }
            }
        }.value

}
