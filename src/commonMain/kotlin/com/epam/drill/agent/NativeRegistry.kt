package com.epam.drill.agent

expect object NativeRegistry {
    fun loadLibrary(path: String)
}
