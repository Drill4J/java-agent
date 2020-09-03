package com.epam.drill.agent

import kotlin.native.concurrent.*
import kotlin.time.*

data class State(
    val startMark: TimeMark = TimeSource.Monotonic.markNow(),
    val alive: Boolean = false, //true if the agent successfully received package prefixes
    val webApps: Map<String, Boolean> = mapOf(),
    val packagePrefixes: List<String> = emptyList()
)

private val _state = AtomicReference(State().freeze()).freeze()

val state: State get() = _state.value

fun updateState(block: State.() -> State): State = _state.value.run(block).freeze().also {
    _state.value = it
}

fun State.allWebAppsInitialized(): Boolean = webApps.isEmpty() || webApps.values.all { it }
