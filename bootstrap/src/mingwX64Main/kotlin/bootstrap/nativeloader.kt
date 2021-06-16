package com.epam.drill.bootstrap

import platform.posix.*

import kotlinx.cinterop.*
import platform.windows.*


fun agentLoad(path: String): Any? = memScoped {
    LoadLibrary!!(path.replace("/", "\\").toLPCWSTR(this).pointed.ptr)
        ?.let { hModule -> GetProcAddress(hModule, "Agent_OnLoad") }
}

private fun String.toLPCWSTR(ms: MemScope): CArrayPointer<UShortVar> {
    val length = this.length
    val allocArray = ms.allocArray<UShortVar>(length.toLong())
    for (i in 0 until length) {
        allocArray[i] = this[i].toShort().toUShort()
    }
    return allocArray
}
