package com.epam.drill.agent

actual object NativeRegistry {
    actual fun loadLibrary(path: String) {
        NativeRegistryStub.loadLibrary(path)
    }
}
