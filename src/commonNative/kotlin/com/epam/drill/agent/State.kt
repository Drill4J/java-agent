package com.epam.drill.agent

import kotlin.native.concurrent.*

private val _state = AtomicReference(State().freeze()).freeze()

var state: State
    get() = _state.value
    set(value) {
        _state.value = value.freeze()
    }


data class State(
    val webApps: Map<String, Boolean> = mapOf(),
    val packagePrefixes: List<String> = emptyList()
)

fun State.allWebAppsInitialized(): Boolean = webApps.isEmpty() || webApps.values.all { it }
