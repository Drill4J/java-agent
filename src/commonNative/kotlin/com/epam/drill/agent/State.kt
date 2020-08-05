package com.epam.drill.agent

import kotlin.native.concurrent.*

private val _state = AtomicReference(State().freeze()).freeze()

var state: State
    get() = _state.value
    set(value) {
        _state.value = value.freeze()
    }

private val _isAsyncApp = AtomicReference(false).freeze()

var isAsyncApp: Boolean
    get() = _isAsyncApp.value
    set(value) {
        _isAsyncApp.value = value.freeze()
    }

data class State(
    val alive: Boolean = false, //true if the agent successfully received package prefixes
    val webApps: Map<String, Boolean> = mapOf(),
    val packagePrefixes: List<String> = emptyList()
)

fun State.allWebAppsInitialized(): Boolean = webApps.isEmpty() || webApps.values.all { it }
