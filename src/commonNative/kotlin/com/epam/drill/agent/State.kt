package com.epam.drill.agent

import kotlin.native.concurrent.*

private val _state = AtomicReference(State().freeze()).freeze()

var state: State
    get() = _state.value
    set(value) {
        _state.value = value.freeze()
    }


data class State(val isWebAppInitialized: Boolean = true)

enum class ApplicationType {
    WAR, EAR;
}
